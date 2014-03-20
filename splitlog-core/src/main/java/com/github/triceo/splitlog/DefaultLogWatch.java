package com.github.triceo.splitlog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.splitters.TailSplitter;

/**
 * Default log watch implementation which provides all the bells and whistles so
 * that the rest of the tool can work together.
 * 
 */
final class DefaultLogWatch implements LogWatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLogWatch.class);
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);

    private final AtomicInteger numberOfActiveFollowers = new AtomicInteger(0);
    private Tailer tailer;
    private final TailSplitter splitter;
    private final AtomicBoolean isTerminated = new AtomicBoolean(false);
    private final Set<AbstractFollower> followers = new LinkedHashSet<AbstractFollower>();
    private final LogWatchTailerListener listener;
    final File watchedFile;
    /**
     * These maps are weak; when a follower stops being used by user code, we do
     * not want these IDs to prevent it from being GC'd. Yet, for as long as the
     * follower is being used, we want to keep the IDs since the follower may
     * still ask for the messages.
     */
    private final Map<Follower, Integer> startingMessageIds = new WeakHashMap<Follower, Integer>(),
            endingMessageIds = new WeakHashMap<Follower, Integer>();

    private final MessageStore messages;
    private final MessageCondition acceptanceCondition;
    private final long delayBetweenReads, delayBetweenSweeps;
    private final int bufferSize;
    private final boolean reopenBetweenReads, ignoreExistingContent;

    protected DefaultLogWatch(final File watchedFile, final TailSplitter splitter, final int capacity,
            final MessageCondition acceptanceCondition, final long delayBetweenReads, final long delayBetweenSweeps,
            final boolean ignoreExistingContent, final boolean reopenBetweenReads, final int bufferSize) {
        this.acceptanceCondition = acceptanceCondition;
        this.messages = new MessageStore(capacity);
        this.splitter = splitter;
        // for the tailer
        this.bufferSize = bufferSize;
        this.delayBetweenSweeps = delayBetweenSweeps;
        this.delayBetweenReads = delayBetweenReads;
        this.reopenBetweenReads = reopenBetweenReads;
        this.ignoreExistingContent = ignoreExistingContent;
        this.watchedFile = watchedFile;
        this.listener = new LogWatchTailerListener(this);
    }

    private MessageBuilder currentlyProcessedMessage = null;

    private int getNumberOfActiveFollowers() {
        return this.numberOfActiveFollowers.get();
    }

    synchronized void addLine(final String line) {
        final boolean isMessageBeingProcessed = this.currentlyProcessedMessage != null;
        if (this.splitter.isStartingLine(line)) {
            // new message begins
            if (isMessageBeingProcessed) { // finish old message
                this.handleCompleteMessage(this.currentlyProcessedMessage);
            }
            // prepare for new message
            this.currentlyProcessedMessage = new MessageBuilder(line);
        } else {
            // continue present message
            if (!isMessageBeingProcessed) {
                // most likely just a garbage immediately after start
                return;
            }
            this.currentlyProcessedMessage.addLine(line);
        }
        this.handleIncomingMessage(this.currentlyProcessedMessage);
    }

    private synchronized void handleIncomingMessage(final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildIntermediate(this.splitter);
        for (final AbstractFollower f : this.followers) {
            f.notifyOfIncomingMessage(message);
        }
    }

    private synchronized void handleUndeliveredMessage(final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildIntermediate(this.splitter);
        for (final AbstractFollower f : this.followers) {
            f.notifyOfUndeliveredMessage(message);
        }
    }

    private synchronized void handleCompleteMessage(final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildFinal(this.splitter);
        final boolean messageAccepted = this.acceptanceCondition.accept(message);
        if (messageAccepted) {
            this.messages.add(message);
        } else {
            DefaultLogWatch.LOGGER.info("Filter rejected message '{}' from file {}.", message, this.watchedFile);
        }
        for (final AbstractFollower f : this.followers) {
            if (messageAccepted) {
                f.notifyOfAcceptedMessage(message);
            } else {
                f.notifyOfRejectedMessage(message);
            }
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
     * @return Unmodifiable map of all the received messages, with keys being
     *         IDs of those messages.
     */
    protected synchronized SortedMap<Integer, Message> getAllMessages(final Follower follower) {
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
            return Collections.unmodifiableSortedMap(new TreeMap<Integer, Message>());
        } else {
            final SortedMap<Integer, Message> messages = new TreeMap<Integer, Message>();
            int id = start;
            for (final Message msg : this.messages.getFromRange(start, end + 1)) {
                messages.put(id, msg);
                id++;
            }
            return Collections.unmodifiableSortedMap(messages);
        }
    }

    /**
     * Get index of the last plus one message that the follower has access to.
     * 
     * @param follower
     *            Tailer in question.
     */
    protected int getEndingMessageId(final Follower follower) {
        return this.endingMessageIds.containsKey(follower) ? this.endingMessageIds.get(follower) : this.messages
                .getLatestMessageId();
    }

    /**
     * If messages have been discarded, the original starting message ID will no
     * longer be valid. therefore, we check for the actual starting ID.
     * 
     * @param follower
     *            Tailer in question.
     */
    protected int getStartingMessageId(final Follower follower) {
        return Math.max(this.messages.getFirstMessageId(), this.startingMessageIds.get(follower));
    }

    /**
     * Get access to the underlying message store.
     * 
     * This method is only intended to be used from within
     * {@link LogWatchMessageSweeper}.
     * 
     * @return Message store used by this class.
     */
    MessageStore getMessageStore() {
        return this.messages;
    }

    /**
     * Will crawl the weak hash maps and make sure we always have the latest
     * information on the availability of messages.
     * 
     * This method is only intended to be used from within
     * {@link LogWatchMessageSweeper}.
     * 
     * @return ID of the very first message that is reachable by any follower in
     *         this watch. -1 when there are no reachable messages.
     */
    synchronized int getFirstReachableMessageId() {
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

    /**
     * <strong>This is not part of the public API.</strong> Purely for purposes
     * of testing the automated message sweep.
     * 
     * @return How many messages there currently are in the internal message
     *         store.
     */
    synchronized int countMessagesInStorage() {
        return this.messages.size();
    }

    @Override
    public boolean isTerminated() {
        return this.isTerminated.get();
    }

    @Override
    public boolean isFollowedBy(final Follower follower) {
        return !this.followers.contains(follower);
    }

    public long getDelayBetweenReads() {
        return this.delayBetweenReads;
    }

    public long getDelayBetweenSweeps() {
        return this.delayBetweenSweeps;
    }

    /**
     * First invocation of the method on an instance will trigger
     * {@link LogWatchMessageSweeper} to be scheduled for periodical sweeping of
     * unreachable messages.
     */
    @Override
    public synchronized Follower follow() {
        if (this.isTerminated()) {
            throw new IllegalStateException("Cannot start tailing on an already terminated LogWatch.");
        }
        if (this.getNumberOfActiveFollowers() < 1) {
            this.startTailer();
        }
        if (this.currentlyRunningSweeper == null) {
            final long delay = this.delayBetweenSweeps;
            this.currentlyRunningSweeper = DefaultLogWatch.TIMER.scheduleWithFixedDelay(
                    new LogWatchMessageSweeper(this), delay, delay, TimeUnit.MILLISECONDS);
            DefaultLogWatch.LOGGER
                    .debug("Scheduled automated unreachable message sweep in log watch for file '{}' to run every {} millisecond(s).",
                            this.watchedFile, delay);
        }
        final int startingMessageId = this.messages.getNextMessageId();
        final AbstractFollower follower = new NonStoringFollower(this);
        this.followers.add(follower);
        this.startingMessageIds.put(follower, startingMessageId);
        this.numberOfActiveFollowers.incrementAndGet();
        return follower;
    }

    /**
     * Invoking this method will cause the running
     * {@link LogWatchMessageSweeper} to be de-scheduled. Any currently present
     * messages will only be removed from memory when this watch instance is
     * removed from memory.
     */
    @Override
    public synchronized boolean terminate() {
        if (this.isTerminated()) {
            return false;
        }
        this.isTerminated.set(true);
        if (this.currentlyProcessedMessage != null) {
            this.handleUndeliveredMessage(this.currentlyProcessedMessage);
        }
        for (final AbstractFollower chunk : new ArrayList<AbstractFollower>(this.followers)) {
            this.unfollow(chunk);
        }
        this.currentlyRunningSweeper.cancel(false);
        this.currentlyRunningSweeper = null;
        return true;
    }

    private final ExecutorService e = Executors.newSingleThreadExecutor();
    private ScheduledFuture<?> currentlyRunningSweeper = null;

    private boolean startTailer() {
        this.tailer = new Tailer(this.watchedFile, this.listener, this.delayBetweenReads, this.ignoreExistingContent,
                this.reopenBetweenReads, this.bufferSize);
        this.e.execute(this.tailer);
        DefaultLogWatch.LOGGER.debug("Started log tailer for file '{}'", this.watchedFile);
        return true;
    }

    private boolean terminateTailer() {
        this.tailer.stop();
        this.tailer = null;
        DefaultLogWatch.LOGGER.debug(
                "Terminated log tailer for file '{}' as the last known Follower has just been terminated.",
                this.watchedFile);
        return true;
    }

    @Override
    public synchronized boolean unfollow(final Follower follower) {
        if (this.followers.remove(follower)) {
            final int endingMessageId = this.messages.getLatestMessageId();
            this.endingMessageIds.put(follower, endingMessageId);
            this.numberOfActiveFollowers.decrementAndGet();
            if (this.getNumberOfActiveFollowers() == 0) {
                this.terminateTailer();
            }
            return true;
        } else {
            return false;
        }
    }
}
