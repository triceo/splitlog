package com.github.triceo.splitlog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;
import com.github.triceo.splitlog.splitters.TailSplitter;

/**
 * Default log watch implementation which provides all the bells and whistles so
 * that the rest of the tool can work together.
 */
final class DefaultLogWatch implements LogWatch {

    private final long delayedTailerStartInMilliseconds;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLogWatch.class);
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);

    private final AtomicInteger numberOfActiveFollowers = new AtomicInteger(0);
    private ScheduledFuture<?> tailer;
    private final TailSplitter splitter;
    private final AtomicBoolean isTerminated = new AtomicBoolean(false);
    private final AtomicInteger numberOfTimesThatTailerWasStarted = new AtomicInteger(0);
    private final Set<AbstractLogWatchFollower> followers = new LinkedHashSet<AbstractLogWatchFollower>();
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
            final boolean ignoreExistingContent, final boolean reopenBetweenReads, final int bufferSize,
            final long delayForTailerStart) {
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
        this.delayedTailerStartInMilliseconds = delayForTailerStart;
    }

    private MessageBuilder currentlyProcessedMessage = null;
    private WeakReference<Message> previousAcceptedMessage;

    private int getNumberOfActiveFollowers() {
        return this.numberOfActiveFollowers.get();
    }

    synchronized void addLine(final String line) {
        final boolean isMessageBeingProcessed = this.currentlyProcessedMessage != null;
        if (this.splitter.isStartingLine(line)) {
            // new message begins
            if (isMessageBeingProcessed) { // finish old message
                final Message completeMessage = this.handleCompleteMessage(this.currentlyProcessedMessage);
                if (completeMessage != null) {
                    this.previousAcceptedMessage = new WeakReference<Message>(completeMessage);
                }
            }
            // prepare for new message
            this.currentlyProcessedMessage = new MessageBuilder(line);
            if (this.previousAcceptedMessage != null) {
                this.currentlyProcessedMessage.setPreviousMessage(this.previousAcceptedMessage.get());
            }
        } else {
            // continue present message
            if (!isMessageBeingProcessed) {
                // most likely just a garbage immediately after start
                return;
            }
            this.currentlyProcessedMessage.add(line);
        }
        this.handleIncomingMessage(this.currentlyProcessedMessage);
    }

    /**
     * Notify {@link Follower}s of a message that is
     * {@link MessageDeliveryStatus#INCOMING}.
     * 
     * @param messageBuilder
     *            Builder to use to construct the message.
     * @return The message that was the subject of notifications.
     */
    private synchronized Message handleIncomingMessage(final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildIntermediate(this.splitter);
        for (final AbstractFollower f : this.followers) {
            f.notifyOfIncomingMessage(message, this);
        }
        return message;
    }

    /**
     * Notify {@link Follower} of a message that could not be delivered fully as
     * the Follower terminated.
     * 
     * @param followe
     *            The followe that was terminated.
     * @param messageBuilder
     *            Builder to use to construct the message.
     * @return The message that was the subject of notifications.
     */
    private synchronized Message handleUndeliveredMessage(final AbstractFollower follower,
        final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildIntermediate(this.splitter);
        follower.notifyOfUndeliveredMessage(message, this);
        return message;
    }

    /**
     * Notify {@link Follower}s of a message that is either
     * {@link MessageDeliveryStatus#ACCEPTED} or
     * {@link MessageDeliveryStatus#REJECTED}.
     * 
     * @param messageBuilder
     *            Builder to use to construct the message.
     * @return The message that was the subject of notifications; null if
     *         rejected.
     */
    private synchronized Message handleCompleteMessage(final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildFinal(this.splitter);
        final boolean messageAccepted = this.acceptanceCondition.accept(message, this);
        if (messageAccepted) {
            this.messages.add(message);
        } else {
            DefaultLogWatch.LOGGER.info("Filter rejected message '{}' from file {}.", message, this.watchedFile);
        }
        for (final AbstractFollower f : this.followers) {
            if (messageAccepted) {
                f.notifyOfAcceptedMessage(message, this);
            } else {
                f.notifyOfRejectedMessage(message, this);
            }
        }
        if (messageAccepted) {
            return message;
        } else {
            return null;
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
    protected int getEndingMessageId(final Follower follower) {
        return this.endingMessageIds.containsKey(follower) ? this.endingMessageIds.get(follower) : this.messages
                .getLatestPosition();
    }

    /**
     * If messages have been discarded, the original starting message ID will no
     * longer be valid. therefore, we check for the actual starting ID.
     * 
     * @param follower
     *            Tailer in question.
     */
    protected int getStartingMessageId(final Follower follower) {
        return Math.max(this.messages.getFirstPosition(), this.startingMessageIds.get(follower));
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
        return this.followers.contains(follower);
    }

    @Override
    public File getWatchedFile() {
        return this.watchedFile;
    }

    public long getDelayBetweenReads() {
        return this.delayBetweenReads;
    }

    public long getDelayBetweenSweeps() {
        return this.delayBetweenSweeps;
    }

    @Override
    public Follower follow() {
        final Follower f = this.followInternal(false);
        long remainingDelay = Long.MAX_VALUE;
        // the tailer may have a delayed start; wait until it actually started
        while (remainingDelay > 0) {
            remainingDelay = this.tailer.getDelay(TimeUnit.MILLISECONDS) + 1;
            if (remainingDelay < 0) {
                continue;
            }
            try {
                DefaultLogWatch.LOGGER.debug("Will wait further {} milliseconds for tailer to be started.");
                Thread.sleep(remainingDelay);
            } catch (final InterruptedException e) {
                // do nothing
            }
        }
        return f;
    }

    /**
     * First invocation of the method on an instance will trigger
     * {@link LogWatchMessageSweeper} to be scheduled for periodical sweeping of
     * unreachable messages.
     * 
     * @param boolean If the tailer needs a delayed start because of
     *        {@link #follow(MessageDeliveryCondition)}, as explained in
     *        {@link LogWatchBuilder#getDelayBeforeTailingStarts()}.
     * @return The follower that follows this log watch from now on.
     */
    private synchronized Follower followInternal(final boolean needsToWait) {
        if (this.isTerminated()) {
            throw new IllegalStateException("Cannot start tailing on an already terminated LogWatch.");
        }
        if (this.currentlyRunningSweeper == null) {
            final long delay = this.delayBetweenSweeps;
            this.currentlyRunningSweeper = DefaultLogWatch.TIMER.scheduleWithFixedDelay(
                    new LogWatchMessageSweeper(this), delay, delay, TimeUnit.MILLISECONDS);
            DefaultLogWatch.LOGGER
                    .debug("Scheduled automated unreachable message sweep in log watch for file '{}' to run every {} millisecond(s).",
                            this.watchedFile, delay);
        }
        final int startingMessageId = this.messages.getNextPosition();
        final AbstractLogWatchFollower follower = new NonStoringFollower(this);
        this.followers.add(follower);
        this.startingMessageIds.put(follower, startingMessageId);
        DefaultLogWatch.LOGGER.info("Registered {} for file '{}'.", follower, this.watchedFile);
        if (this.numberOfActiveFollowers.incrementAndGet() == 1) {
            this.startTailer(needsToWait);
        }
        return follower;
    }

    @Override
    public Pair<Follower, Message> follow(final MessageDeliveryCondition waitFor) {
        final Follower f = this.followInternal(true);
        return ImmutablePair.of(f, f.waitFor(waitFor));
    }

    @Override
    public Pair<Follower, Message> follow(final MessageDeliveryCondition waitFor, final long howLong,
        final TimeUnit unit) {
        final Follower f = this.followInternal(true);
        return ImmutablePair.of(f, f.waitFor(waitFor, howLong, unit));
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
        for (final AbstractLogWatchFollower chunk : new ArrayList<AbstractLogWatchFollower>(this.followers)) {
            this.unfollow(chunk);
        }
        this.currentlyRunningSweeper.cancel(false);
        // remove references to stuff that is no longer useful
        this.currentlyRunningSweeper = null;
        this.currentlyProcessedMessage = null;
        this.previousAcceptedMessage = null;
        return true;
    }

    private final ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> currentlyRunningSweeper = null;

    private boolean willReadFromEnd() {
        if (this.numberOfTimesThatTailerWasStarted.get() > 0) {
            return true;
        } else {
            return this.ignoreExistingContent;
        }
    }

    /**
     * Start the tailer on a separate thread. Only when a tailer is running can
     * {@link Follower}s be notified of new {@link Message}s from the log.
     * 
     * @param needsToWait
     *            Whether the start of the tailer needs to be delayed by
     *            {@link #delayedTailerStartInMilliseconds} milliseconds.
     * @return Whether or not the tailer start was scheduled.
     */
    private boolean startTailer(final boolean needsToWait) {
        if (this.tailer != null) {
            DefaultLogWatch.LOGGER.debug("Tailer already running, therefore not starting.");
            return false;
        }
        final Tailer t = new Tailer(this.watchedFile, new LogWatchTailerListener(this), this.delayBetweenReads,
                this.willReadFromEnd(), this.reopenBetweenReads, this.bufferSize);
        final long delay = needsToWait ? this.delayedTailerStartInMilliseconds : 0;
        this.tailer = this.e.schedule(t, delay, TimeUnit.MILLISECONDS);
        if (this.numberOfTimesThatTailerWasStarted.getAndIncrement() == 0) {
            DefaultLogWatch.LOGGER.debug("Scheduling log tailer for file '{}' with delay of {} milliseconds.",
                    this.watchedFile, delay);
        } else {
            DefaultLogWatch.LOGGER.debug("Re-scheduling log tailer for file '{}' with delay of {} milliseconds.",
                    this.watchedFile, delay);
        }
        return true;
    }

    private boolean terminateTailer() {
        if (this.tailer == null) {
            DefaultLogWatch.LOGGER.debug("Tailer not running, therefore not terminating.");
            return false;
        }
        // forcibly terminate tailer
        this.tailer.cancel(true);
        this.tailer = null;
        // cancel whatever message processing that was ongoing
        this.currentlyProcessedMessage = null;
        DefaultLogWatch.LOGGER.debug(
                "Terminated log tailer for file '{}' as the last known Follower has just been terminated.",
                this.watchedFile);
        return true;
    }

    @Override
    public synchronized boolean unfollow(final Follower follower) {
        if (this.followers.remove(follower)) {
            if (this.currentlyProcessedMessage != null) {
                this.handleUndeliveredMessage((AbstractFollower) follower, this.currentlyProcessedMessage);
            }
            DefaultLogWatch.LOGGER.info("Unregistered {} for file '{}'.", follower, this.watchedFile);
            final int endingMessageId = this.messages.getLatestPosition();
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
