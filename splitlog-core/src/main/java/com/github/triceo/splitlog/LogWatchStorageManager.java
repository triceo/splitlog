package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicLong numberOfActiveFollowers = new AtomicLong(0);
    /**
     * These maps are weak; when a follower stops being used by user code, we do
     * not want these IDs to prevent it from being GC'd. Yet, for as long as the
     * follower is being used, we want to keep the IDs since the follower may
     * still ask for the messages.
     */
    private final Map<Follower, Integer> startingMessageIds = new WeakHashMap<Follower, Integer>(),
            endingMessageIds = new WeakHashMap<Follower, Integer>();

    public LogWatchStorageManager(final LogWatch watch, final LogWatchBuilder builder) {
        this.logWatch = watch;
        this.messages = new MessageStore(builder.getCapacityLimit());
        this.acceptanceCondition = builder.getStorageCondition();
    }

    public synchronized void followerStarted(final Follower follower) {
        if (this.numberOfActiveFollowers.incrementAndGet() == 1) {
            LogWatchStorageManager.LOGGER.info("New follower registered. Messages can be received.");
        }
        final int startingMessageId = this.messages.getNextPosition();
        LogWatchStorageManager.LOGGER.info("First message position is {} for {}.", startingMessageId, follower);
        this.startingMessageIds.put(follower, startingMessageId);
    }

    public synchronized void followerTerminated(final Follower follower) {
        final int endingMessageId = this.messages.getLatestPosition();
        LogWatchStorageManager.LOGGER.info("Last message position is {} for {}.", endingMessageId, follower);
        this.endingMessageIds.put(follower, endingMessageId);
        if (this.numberOfActiveFollowers.decrementAndGet() == 0) {
            LogWatchStorageManager.LOGGER.info("Last remaining follower terminated. No messages can be received.");
        }
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
        final int start = this.getStartingMessageId(follower);
        // get the expected ending message ID
        final int end = this.getEndingMessageId(follower);
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
     *            Tailer in question.
     */
    private int getEndingMessageId(final Follower follower) {
        return this.endingMessageIds.containsKey(follower) ? this.endingMessageIds.get(follower) : this.messages
                .getLatestPosition();
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
        int minId = Integer.MAX_VALUE;
        int valueCount = 0;
        for (final Integer id : this.startingMessageIds.values()) {
            minId = Math.min(minId, id);
            valueCount++;
        }
        if (valueCount == 0) {
            return -1;
        } else {
            return minId;
        }
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
     * If messages have been discarded, the original starting message ID will no
     * longer be valid. therefore, we check for the actual starting ID.
     *
     * @param follower
     *            Tailer in question.
     */
    private int getStartingMessageId(final Follower follower) {
        return Math.max(this.messages.getFirstPosition(), this.startingMessageIds.get(follower));
    }

    public synchronized boolean registerMessage(final Message message, final LogWatch source) {
        if (source != this.logWatch) {
            throw new IllegalStateException("Sources don't match.");
        }
        final boolean messageAccepted = this.acceptanceCondition.accept(message);
        if (this.numberOfActiveFollowers.get() == 0) {
            LogWatchStorageManager.LOGGER.info("Message thrown away as there are no followers: {}.", message);
        } else if (messageAccepted) {
            LogWatchStorageManager.LOGGER.info("Message '{}' stored into {}.", message, source);
            this.messages.add(message);
        }
        return messageAccepted;
    }

}
