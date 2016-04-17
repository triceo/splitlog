package com.github.triceo.splitlog;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

final class LogWatchStorageManager {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(LogWatchStorageManager.class);
    private final SimpleMessageCondition acceptanceCondition;
    private final LogWatch logWatch;
    private final MessageStore messages;
    private final Object2IntMap<Follower> runningFollowerStartMarks = new Object2IntLinkedOpenHashMap<>();
    private final LogWatchStorageSweeper sweeping;
    /**
     * This map is weak; when a follower stops being used by user code, we do
     * not want this map to prevent it from being GC'd. Yet, for as long as the
     * follower is being used, we want to keep the IDs since the follower may
     * still ask for the messages.
     */
    private final Map<Follower, int[]> terminatedFollowerRanges = new WeakHashMap<>();

    public LogWatchStorageManager(final LogWatch watch, final LogWatchBuilder builder) {
        this.logWatch = watch;
        this.messages = new MessageStore(builder.getCapacityLimit());
        this.acceptanceCondition = builder.getStorageCondition();
        this.sweeping = new LogWatchStorageSweeper(this, builder);
    }

    public synchronized boolean followerStarted(final Follower follower) {
        if (this.isFollowerActive(follower) || this.isFollowerTerminated(follower)) {
            return false;
        }
        final int startingMessageId = this.messages.getNextPosition();
        LogWatchStorageManager.LOGGER.info("First message position is {} for {}.", startingMessageId, follower);
        this.runningFollowerStartMarks.put(follower, startingMessageId);
        if (this.runningFollowerStartMarks.size() == 1) {
            LogWatchStorageManager.LOGGER.info("New follower registered. Messages can be received.");
        }
        return true;
    }

    public synchronized boolean followerTerminated(final Follower follower) {
        if (!this.isFollowerActive(follower)) {
            return false;
        }
        final int startingMessageId = this.runningFollowerStartMarks.removeInt(follower);
        final int endingMessageId = this.messages.getLatestPosition();
        LogWatchStorageManager.LOGGER.info("Last message position is {} for {}.", endingMessageId, follower);
        this.terminatedFollowerRanges.put(follower, new int[]{startingMessageId, endingMessageId});
        if (this.runningFollowerStartMarks.size() == 0) {
            LogWatchStorageManager.LOGGER.info("Last remaining follower terminated. No messages can be received.");
        }
        return true;
    }

    /**
     * Return all messages that have been sent to the follower, from its start
     * until either its termination or to this moment, whichever is relevant.
     *
     * This method is synchronized so that the modification of the underlying
     * message store in {@link #addLine(String)} and the reading of this store
     * is mutually excluded. Otherwise, there is a possibility of message ID
     * mess in the discarding case.
     *
     * @param follower
     *            The follower in question.
     * @return Unmodifiable list of all the received messages, in the order
     *         received.
     */
    protected synchronized List<Message> getAllMessages(final Follower follower) {
        final int end = this.getEndingMessageId(follower);
        /*
         * If messages have been discarded, the original starting message ID
         * will no longer be valid. Therefore, we check for the actual starting
         * ID.
         */
        final int start = Math.max(this.messages.getFirstPosition(), this.getStartingMessageId(follower));
        if (start > end) {
            /*
             * in case some messages have been discarded, the actual start may
             * get ahead of the expected end. this would have caused an
             * exception within the message store, and so we handle it here and
             * return an empty list. this is exactly correct, as if the end is
             * before the first message in the store, there really is nothing to
             * return.
             */
            return Collections.unmodifiableList(Collections.<Message> emptyList());
        } else {
            return Collections.unmodifiableList(this.messages.getFromRange(start, end + 1));
        }
    }

    /**
     * Get index of the last plus one message that the follower has access to.
     *
     * @param follower
     *            Follower in question.
     * @return Ending message ID after {@link #followerTerminated(Follower)}.
     *         {@link MessageStore#getLatestPosition()} between
     *         {@link #followerStarted(Follower)} and
     *         {@link #followerTerminated(Follower)}. Will throw an exception
     *         otherwise.
     */
    private synchronized int getEndingMessageId(final Follower follower) {
        if (this.isFollowerActive(follower)) {
            return this.messages.getLatestPosition();
        } else if (this.isFollowerTerminated(follower)) {
            return this.terminatedFollowerRanges.get(follower)[1];
        } else {
            throw new IllegalStateException("Follower never before seen.");
        }
    }

    /**
     * Will crawl the weak hash maps and make sure we always have the latest
     * information on the availability of messages.
     *
     * This method is only intended to be used from within
     * {@link LogWatchStorageSweeper}.
     *
     * @return ID of the very first message that is reachable by any follower in
     *         this logWatch. -1 when there are no reachable messages.
     */
    protected synchronized int getFirstReachableMessageId() {
        final boolean followersRunning = !this.runningFollowerStartMarks.isEmpty();
        if (!followersRunning && this.terminatedFollowerRanges.isEmpty()) {
            // no followers present; no reachable messages
            return -1;
        }
        final IntSortedSet set = new IntAVLTreeSet(this.runningFollowerStartMarks.values());
        if (!set.isEmpty()) {
            final int first = this.messages.getFirstPosition();
            if (set.firstInt() <= first) {
                /*
                 * cannot go below first position; any other calculation
                 * unnecessary
                 */
                return first;
            }
        }
        set.addAll(this.terminatedFollowerRanges.values().stream().map(pair -> pair[0]).collect(Collectors.toList()));
        return set.firstInt();
    }

    public LogWatch getLogWatch() {
        return this.logWatch;
    }

    /**
     * Get access to the underlying message store.
     *
     * This method is only intended to be used from within
     * {@link LogWatchStorageSweeper}.
     *
     * @return Message store used by this class.
     */
    protected MessageStore getMessageStore() {
        return this.messages;
    }

    /**
     * For a given follower, return the starting mark.
     *
     * @param follower
     *            Tailer in question.
     * @return Starting message ID, if after {@link #followerStarted(Follower)}.
     *         Will throw an exception otherwise.
     */
    private synchronized int getStartingMessageId(final Follower follower) {
        if (this.isFollowerActive(follower)) {
            return this.runningFollowerStartMarks.getInt(follower);
        } else if (this.isFollowerTerminated(follower)) {
            return this.terminatedFollowerRanges.get(follower)[0];
        } else {
            throw new IllegalStateException("Follower never before seen.");
        }
    }

    /**
     * Whether {@link #followerStarted(Follower)} has been called and
     * {@link #followerTerminated(Follower)} has not.
     *
     * @param follower
     *            Follower in question.
     * @return True if inbetween those two calls for this particular follower.
     */
    public synchronized boolean isFollowerActive(final Follower follower) {
        return this.runningFollowerStartMarks.containsKey(follower);
    }

    /**
     * Whether or not {@link #followerTerminated(Follower)} was called on the
     * follower with a positive result.
     *
     * @param follower
     *            Follower in question.
     * @return True if called and returned true. May be unpredictable, since it
     *         will look up references to terminated followers within a weak has
     *         map.
     */
    public synchronized boolean isFollowerTerminated(final Follower follower) {
        return this.terminatedFollowerRanges.containsKey(follower);
    }

    /**
     * Will mean the end of the storage, including the termination of sweeping.
     */
    public synchronized void logWatchTerminated() {
        final Set<Follower> followersToTerminate = new ObjectLinkedOpenHashSet<>(this.runningFollowerStartMarks.keySet());
        followersToTerminate.forEach(this::followerTerminated);
        this.sweeping.stop();
    }

    public synchronized boolean registerMessage(final Message message, final LogWatch source) {
        if (source != this.logWatch) {
            throw new IllegalStateException("Sources don't match.");
        }
        final boolean messageAccepted = this.acceptanceCondition.accept(message);
        if (this.runningFollowerStartMarks.size() == 0) {
            LogWatchStorageManager.LOGGER.info("Message thrown away as there are no followers: {}.", message);
        } else if (messageAccepted) {
            LogWatchStorageManager.LOGGER.info("Message '{}' stored into {}.", message, source);
            this.messages.add(message);
            this.sweeping.start();
        }
        return messageAccepted;
    }

}
