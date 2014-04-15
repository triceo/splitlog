package com.github.triceo.splitlog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.api.TailSplitter;

/**
 * Default log watch implementation which provides all the bells and whistles so
 * that the rest of the tool can work together.
 */
final class DefaultLogWatch implements LogWatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLogWatch.class);

    private MessageBuilder currentlyProcessedMessage;
    private final BidiMap<String, MessageMeasure<? extends Number, Follower>> handingDown = new DualHashBidiMap<String, MessageMeasure<? extends Number, Follower>>();
    private boolean isTerminated = false;
    private final MeasuringConsumerManager<LogWatch> messaging = new MeasuringConsumerManager<LogWatch>(this);
    private final AtomicInteger numberOfActiveFollowers = new AtomicInteger(0);
    private WeakReference<Message> previousAcceptedMessage;
    private final TailSplitter splitter;
    private final LogWatchStorageManager storage;
    private final LogWatchSweepingManager sweeping;
    private final LogWatchTailingManager tailing;
    private final File watchedFile;

    protected DefaultLogWatch(final File watchedFile, final TailSplitter splitter, final int capacity,
            final SimpleMessageCondition acceptanceCondition, final long delayBetweenReads,
        final long delayBetweenSweeps, final boolean ignoreExistingContent, final boolean reopenBetweenReads,
        final int bufferSize, final long delayForTailerStart) {
        this.splitter = splitter;
        this.storage = new LogWatchStorageManager(this, capacity, acceptanceCondition);
        this.tailing = new LogWatchTailingManager(this, delayBetweenReads, delayForTailerStart, ignoreExistingContent,
                reopenBetweenReads, bufferSize);
        this.sweeping = new LogWatchSweepingManager(this.storage, delayBetweenSweeps);
        this.watchedFile = watchedFile;
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
     * <strong>This is not part of the public API.</strong> Purely for purposes
     * of testing the automated message sweep.
     *
     * @return How many messages there currently are in the internal message
     *         store.
     */
    int countMessagesInStorage() {
        return this.storage.getMessageStore().size();
    }

    /**
     * Return all messages that have been sent to a given {@link Follower}, from
     * its {@link #startFollowing()} until either its
     * {@link #stopFollowing(Follower)} or to this moment, whichever is
     * relevant.
     *
     * @param follower
     *            The follower in question.
     * @return Unmodifiable list of all the received messages, in the order
     *         received.
     */
    protected List<Message> getAllMessages(final Follower follower) {
        return this.storage.getAllMessages(follower);
    }

    @Override
    public MessageMetric<? extends Number, LogWatch> getMetric(final String id) {
        return this.messaging.getMetric(id);
    }

    @Override
    public String getMetricId(final MessageMetric<? extends Number, LogWatch> measure) {
        return this.messaging.getMetricId(measure);
    }

    @Override
    public File getWatchedFile() {
        return this.watchedFile;
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
        final boolean messageAccepted = this.storage.registerMessage(message, this);
        final MessageDeliveryStatus status = messageAccepted ? MessageDeliveryStatus.ACCEPTED
                : MessageDeliveryStatus.REJECTED;
        this.messaging.messageReceived(message, status, this);
        return messageAccepted ? message : null;
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
        this.messaging.messageReceived(message, MessageDeliveryStatus.INCOMING, this);
        return message;
    }

    /**
     * Notify {@link Follower} of a message that could not be delivered fully as
     * the Follower terminated. Will not notify local messaging.
     *
     * @param follower
     *            The follower that was terminated.
     * @param messageBuilder
     *            Builder to use to construct the message.
     * @return The message that was the subject of notifications.
     */
    private synchronized Message handleUndeliveredMessage(final AbstractLogWatchFollower follower,
        final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildIntermediate(this.splitter);
        follower.messageReceived(message, MessageDeliveryStatus.INCOMPLETE, this);
        return message;
    }

    @Override
    public boolean isConsuming(final MessageConsumer<LogWatch> consumer) {
        return this.messaging.isConsuming(consumer);
    }

    @Override
    public synchronized boolean isFollowedBy(final Follower follower) {
        return this.isConsuming(follower);
    }

    @Override
    public boolean isHandingDown(final MessageMeasure<? extends Number, Follower> measure) {
        return this.handingDown.containsValue(measure);
    }

    @Override
    public boolean isHandingDown(final String id) {
        return this.handingDown.containsKey(id);
    }

    @Override
    public boolean isMeasuring(final MessageMetric<? extends Number, LogWatch> metric) {
        return this.messaging.isMeasuring(metric);
    }

    @Override
    public boolean isMeasuring(final String id) {
        return this.messaging.isMeasuring(id);
    }

    @Override
    public synchronized boolean isTerminated() {
        return this.isTerminated;
    }

    @Override
    public boolean startConsuming(final MessageConsumer<LogWatch> consumer) {
        return this.messaging.startConsuming(consumer);
    }

    @Override
    public Follower startFollowing() {
        final Follower f = this.startFollowingActually(false);
        this.tailing.waitUntilStarted();
        return f;
    }

    @Override
    public Pair<Follower, Message> startFollowing(final MidDeliveryMessageCondition<LogWatch> waitFor) {
        final Follower f = this.startFollowingActually(true);
        return ImmutablePair.of(f, f.waitFor(waitFor));
    }

    @Override
    public Pair<Follower, Message> startFollowing(final MidDeliveryMessageCondition<LogWatch> waitFor,
            final long howLong, final TimeUnit unit) {
        final Follower f = this.startFollowingActually(true);
        return ImmutablePair.of(f, f.waitFor(waitFor, howLong, unit));
    }

    /**
     * First invocation of the method on an instance will trigger
     * {@link LogWatchStorageSweeper} to be scheduled for periodical sweeping of
     * unreachable messages.
     *
     * @param boolean If the tailer needs a delayed start because of
     *        {@link #startFollowing(MidDeliveryMessageCondition)}, as explained
     *        in {@link LogWatchBuilder#getDelayBeforeTailingStarts()}.
     * @return The follower that follows this log watch from now on.
     */
    private synchronized Follower startFollowingActually(final boolean needsToWait) {
        if (this.isTerminated()) {
            throw new IllegalStateException("Cannot start tailing on an already terminated LogWatch.");
        }
        this.sweeping.start();
        // assemble list of messaging to be handing down and then the follower
        final List<Pair<String, MessageMeasure<? extends Number, Follower>>> pairs = new ArrayList<Pair<String, MessageMeasure<? extends Number, Follower>>>();
        for (final BidiMap.Entry<String, MessageMeasure<? extends Number, Follower>> entry : this.handingDown
                .entrySet()) {
            pairs.add(ImmutablePair.<String, MessageMeasure<? extends Number, Follower>> of(entry.getKey(),
                    entry.getValue()));
        }
        // register the follower
        final AbstractLogWatchFollower follower = new NonStoringFollower(this, pairs);
        this.messaging.startConsuming(follower);
        this.storage.followerStarted(follower);
        DefaultLogWatch.LOGGER.info("Registered {} for {}.", follower, this);
        if (this.numberOfActiveFollowers.incrementAndGet() == 1) {
            // we have listeners, let's start tailing
            this.tailing.start(needsToWait);
        }
        return follower;
    }

    @Override
    public synchronized boolean startHandingDown(final MessageMeasure<? extends Number, Follower> measure,
        final String id) {
        if (measure == null) {
            throw new IllegalArgumentException("Measure may not be null.");
        } else if (id == null) {
            throw new IllegalArgumentException("ID may not be null.");
        } else if (this.handingDown.containsKey(id) || this.handingDown.containsValue(measure)) {
            return false;
        }
        this.handingDown.put(id, measure);
        return true;
    }

    @Override
    public <T extends Number> MessageMetric<T, LogWatch> startMeasuring(final MessageMeasure<T, LogWatch> measure,
            final String id) {
        if (!this.isTerminated()) {
            throw new IllegalStateException("Cannot start measuring, log watch already terminated.");
        }
        return this.messaging.startMeasuring(measure, id);
    }

    @Override
    public boolean stopConsuming(final MessageConsumer<LogWatch> consumer) {
        return this.messaging.stopConsuming(consumer);
    }

    @Override
    public synchronized boolean stopFollowing(final Follower follower) {
        if (!this.isFollowedBy(follower)) {
            return false;
        }
        if (this.currentlyProcessedMessage != null) {
            this.handleUndeliveredMessage((AbstractLogWatchFollower) follower, this.currentlyProcessedMessage);
        }
        this.messaging.stopConsuming(follower);
        this.storage.followerTerminated(follower);
        DefaultLogWatch.LOGGER.info("Unregistered {} for {}.", follower, this);
        if (this.numberOfActiveFollowers.decrementAndGet() == 0) {
            this.tailing.stop();
            this.currentlyProcessedMessage = null;
        }
        return true;
    }

    @Override
    public synchronized boolean stopHandingDown(final MessageMeasure<? extends Number, Follower> measure) {
        return (this.handingDown.removeValue(measure) != null);
    }

    @Override
    public synchronized boolean stopHandingDown(final String id) {
        return (this.handingDown.remove(id) != null);
    }

    @Override
    public boolean stopMeasuring(final MessageMetric<? extends Number, LogWatch> metric) {
        return this.messaging.stopMeasuring(metric);
    }

    @Override
    public boolean stopMeasuring(final String id) {
        return this.messaging.stopMeasuring(id);
    }

    /**
     * Invoking this method will cause the running
     * {@link LogWatchStorageSweeper} to be de-scheduled. Any currently present
     * {@link Message}s will only be removed from memory when this watch
     * instance is removed from memory.
     */
    @Override
    public synchronized boolean terminate() {
        if (this.isTerminated()) {
            return false;
        }
        this.isTerminated = true;
        this.messaging.stop();
        this.handingDown.clear();
        this.sweeping.stop();
        this.previousAcceptedMessage = null;
        return true;
    }

    @Override
    public String toString() {
        // properly size the builder
        final String filename = this.watchedFile.toString();
        final int length = 40 + filename.length();
        final StringBuilder builder = new StringBuilder(length);
        // build the string
        builder.append("LogWatch [file=");
        builder.append(filename);
        if (this.isTerminated()) {
            builder.append(", terminated");
        } else {
            builder.append(", running");
        }
        builder.append(']');
        return builder.toString();
    }

}
