package com.github.triceo.splitlog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.api.TailSplitter;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

/**
 * Default log watch implementation which provides all the bells and whistles so
 * that the rest of the tool can work together.
 *
 * The tailer thread will only be started after {@link #startFollowing()} or
 * {@link #startConsuming(MessageListener)} is called. Subsequently, it will
 * only be stopped after there no more running {@link Follower}s or
 * {@link MessageConsumer}s.
 */
final class DefaultLogWatch implements LogWatch {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(DefaultLogWatch.class);
    static {
        /*
         * intentionally using the original logger so that this message can not
         * be silenced
         */
        final Logger l = LoggerFactory.getLogger(DefaultLogWatch.class);
        if (SplitlogLoggerFactory.isLoggingEnabled()) {
            l.info("Splitlog's internal logging system can be disabled by setting '{}' system property to '{}'.",
                    SplitlogLoggerFactory.LOGGING_PROPERTY_NAME, SplitlogLoggerFactory.OFF_STATE);
        } else {
            l.warn("This will be the last message from Splitlog, unless you enable Splitlog's internal logging system by setting '{}' system property to '{}'.",
                    SplitlogLoggerFactory.LOGGING_PROPERTY_NAME, SplitlogLoggerFactory.ON_STATE);
        }
    }

    private final ConsumerManager<LogWatch> consumers = new ConsumerManager<LogWatch>(this);
    private MessageBuilder currentlyProcessedMessage;
    private final SimpleMessageCondition gateCondition;
    private final BidiMap<String, MessageMeasure<? extends Number, Follower>> handingDown = new DualHashBidiMap<String, MessageMeasure<? extends Number, Follower>>();
    private boolean isTerminated = false;
    private WeakReference<Message> previousAcceptedMessage;
    private final TailSplitter splitter;
    private final LogWatchStorageManager storage;
    private final LogWatchSweepingManager sweeping;
    private final LogWatchTailingManager tailing;
    private final long uniqueId = DefaultLogWatch.ID_GENERATOR.getAndIncrement();
    private final File watchedFile;

    protected DefaultLogWatch(final File watchedFile, final TailSplitter splitter, final int capacity,
            final SimpleMessageCondition gateCondition, final SimpleMessageCondition acceptanceCondition,
            final long delayBetweenReads, final long delayBetweenSweeps, final boolean ignoreExistingContent,
            final boolean reopenBetweenReads, final int bufferSize) {
        this.splitter = splitter;
        this.gateCondition = gateCondition;
        this.storage = new LogWatchStorageManager(this, capacity, acceptanceCondition);
        this.tailing = new LogWatchTailingManager(this, delayBetweenReads, ignoreExistingContent, reopenBetweenReads,
                bufferSize);
        this.sweeping = new LogWatchSweepingManager(this.storage, delayBetweenSweeps);
        this.watchedFile = watchedFile;
    }

    synchronized void addLine(final String line) {
        final boolean isMessageBeingProcessed = this.currentlyProcessedMessage != null;
        if (this.splitter.isStartingLine(line)) {
            // new message begins
            if (isMessageBeingProcessed) { // finish old message
                final Message completeMessage = this.currentlyProcessedMessage.buildFinal(this.splitter);
                final MessageDeliveryStatus accepted = this.handleCompleteMessage(completeMessage);
                if (accepted == null) {
                    DefaultLogWatch.LOGGER.info("Message {} rejected at the gate to {}.", completeMessage, this);
                } else if (accepted == MessageDeliveryStatus.ACCEPTED) {
                    this.previousAcceptedMessage = new WeakReference<Message>(completeMessage);
                } else {
                    DefaultLogWatch.LOGGER.info("Message {} rejected from storage in {}.", completeMessage, this);
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
        this.handleIncomingMessage(this.currentlyProcessedMessage.buildIntermediate(this.splitter));
    }

    @Override
    public int countConsumers() {
        return this.consumers.countConsumers();
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

    @Override
    public int countMetrics() {
        return this.consumers.countMetrics();
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
        return this.consumers.getMetric(id);
    }

    @Override
    public String getMetricId(final MessageMetric<? extends Number, LogWatch> measure) {
        return this.consumers.getMetricId(measure);
    }

    public long getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public File getWatchedFile() {
        return this.watchedFile;
    }

    /**
     * Notify {@link MessageConsumer}s of a message that is either
     * {@link MessageDeliveryStatus#ACCEPTED} or
     * {@link MessageDeliveryStatus#REJECTED}.
     *
     * @param message
     *            The message in question.
     * @return Null if stopped at the gate by
     *         {@link LogWatchBuilder#getGateCondition()},
     *         {@link MessageDeliveryStatus#ACCEPTED} if accepted in
     *         {@link LogWatchBuilder#getStorageCondition()},
     *         {@link MessageDeliveryStatus#REJECTED} otherwise.
     */
    private synchronized MessageDeliveryStatus handleCompleteMessage(final Message message) {
        if (!this.hasToLetMessageThroughTheGate(message)) {
            return null;
        }
        final boolean messageAccepted = this.storage.registerMessage(message, this);
        final MessageDeliveryStatus status = messageAccepted ? MessageDeliveryStatus.ACCEPTED
                : MessageDeliveryStatus.REJECTED;
        this.consumers.messageReceived(message, status, this);
        return status;
    }

    /**
     * Notify {@link MessageConsumer}s of a message that is
     * {@link MessageDeliveryStatus#INCOMING}.
     *
     * @param message
     *            The message in question.
     * @return True if the message was passed to {@link MessageConsumer}s, false
     *         if stopped at the gate by
     *         {@link LogWatchBuilder#getGateCondition()}.
     */
    private synchronized boolean handleIncomingMessage(final Message message) {
        if (!this.hasToLetMessageThroughTheGate(message)) {
            return false;
        }
        this.consumers.messageReceived(message, MessageDeliveryStatus.INCOMING, this);
        return true;
    }

    /**
     * Notify {@link Follower} of a message that could not be delivered fully as
     * the Follower terminated. Will not notify local consumers.
     *
     * @param follower
     *            The follower that was terminated.
     * @param message
     *            The message in question.
     * @return True if the message was passed to the {@link Follower}, false if
     *         stopped at the gate by {@link LogWatchBuilder#getGateCondition()}
     *         .
     */
    private synchronized boolean handleUndeliveredMessage(final Follower follower, final Message message) {
        if (!this.hasToLetMessageThroughTheGate(message)) {
            return false;
        }
        // FIXME should inform other consumers? or just metrics on LogWatch?
        follower.messageReceived(message, MessageDeliveryStatus.INCOMPLETE, this);
        return true;
    }

    private boolean hasToLetMessageThroughTheGate(final Message message) {
        if (this.gateCondition.accept(message)) {
            return true;
        } else {
            DefaultLogWatch.LOGGER.info("Message '{}' stopped at the gate in {}.", message, this);
            return false;
        }
    }

    @Override
    public boolean isConsuming(final MessageConsumer<LogWatch> consumer) {
        return this.consumers.isConsuming(consumer);
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
        return this.consumers.isMeasuring(metric);
    }

    @Override
    public boolean isMeasuring(final String id) {
        return this.consumers.isMeasuring(id);
    }

    @Override
    public synchronized boolean isTerminated() {
        return this.isTerminated;
    }

    @Override
    public synchronized MessageConsumer<LogWatch> startConsuming(final MessageListener<LogWatch> consumer) {
        final MessageConsumer<LogWatch> result = this.consumers.startConsuming(consumer);
        this.tailing.start();
        return result;
    }

    @Override
    public Follower startFollowing() {
        return this.startFollowingActually(null).getKey();
    }

    @Override
    public Pair<Follower, Message> startFollowing(final MidDeliveryMessageCondition<LogWatch> waitFor) {
        final Pair<Follower, Future<Message>> pair = this.startFollowingActually(waitFor);
        final Follower f = pair.getKey();
        try {
            return ImmutablePair.of(f, pair.getValue().get());
        } catch (final Exception e) {
            return ImmutablePair.of(f, null);
        }
    }

    @Override
    public Pair<Follower, Message> startFollowing(final MidDeliveryMessageCondition<LogWatch> waitFor,
        final long howLong, final TimeUnit unit) {
        final Pair<Follower, Future<Message>> pair = this.startFollowingActually(waitFor);
        final Follower f = pair.getKey();
        try {
            return ImmutablePair.of(f, pair.getValue().get(howLong, unit));
        } catch (final Exception e) {
            return ImmutablePair.of(f, null);
        }
    }

    /**
     * @param boolean If the tailer needs a delayed start because of
     *        {@link #startFollowing(MidDeliveryMessageCondition)}, as explained
     *        in {@link LogWatchBuilder#getDelayBeforeTailingStarts()}.
     * @return The follower that follows this log watch from now on.
     */
    private synchronized Pair<Follower, Future<Message>> startFollowingActually(
        final MidDeliveryMessageCondition<LogWatch> condition) {
        if (this.isTerminated()) {
            throw new IllegalStateException("Cannot start tailing on an already terminated LogWatch.");
        }
        this.sweeping.start();
        // assemble list of consumers to be handing down and then the follower
        final List<Pair<String, MessageMeasure<? extends Number, Follower>>> pairs = new ArrayList<Pair<String, MessageMeasure<? extends Number, Follower>>>();
        for (final BidiMap.Entry<String, MessageMeasure<? extends Number, Follower>> entry : this.handingDown
                .entrySet()) {
            pairs.add(ImmutablePair.<String, MessageMeasure<? extends Number, Follower>> of(entry.getKey(),
                    entry.getValue()));
        }
        // register the follower
        final Follower follower = new DefaultFollower(this, pairs);
        final Future<Message> expectation = condition == null ? null : follower.expect(condition);
        this.consumers.registerConsumer(follower);
        this.storage.followerStarted(follower);
        DefaultLogWatch.LOGGER.info("Registered {} for {}.", follower, this);
        this.tailing.start();
        return ImmutablePair.of(follower, expectation);
    }

    @Override
    public Pair<Follower, Future<Message>> startFollowingWithExpectation(
        final MidDeliveryMessageCondition<LogWatch> waitFor) {
        final Pair<Follower, Future<Message>> pair = this.startFollowingActually(waitFor);
        return ImmutablePair.of(pair.getKey(), pair.getValue());
    }

    @Override
    public synchronized boolean startHandingDown(final MessageMeasure<? extends Number, Follower> measure,
        final String id) {
        if (this.isTerminated()) {
            throw new IllegalStateException("Log watch already terminated.");
        } else if (measure == null) {
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
        return this.consumers.startMeasuring(measure, id);
    }

    @Override
    public synchronized boolean stopConsuming(final MessageConsumer<LogWatch> consumer) {
        final boolean result = this.consumers.stopConsuming(consumer);
        this.stopTailingIfNecessary();
        return result;
    }

    @Override
    public synchronized boolean stopFollowing(final Follower follower) {
        if (!this.isFollowedBy(follower)) {
            return false;
        }
        if (this.currentlyProcessedMessage != null) {
            this.handleUndeliveredMessage(follower, this.currentlyProcessedMessage.buildIntermediate(this.splitter));
        }
        this.consumers.stopConsuming(follower);
        this.storage.followerTerminated(follower);
        DefaultLogWatch.LOGGER.info("Unregistered {} for {}.", follower, this);
        this.stopTailingIfNecessary();
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
        return this.consumers.stopMeasuring(metric);
    }

    @Override
    public boolean stopMeasuring(final String id) {
        return this.consumers.stopMeasuring(id);
    }

    private synchronized void stopTailingIfNecessary() {
        if (!this.tailing.isRunning()) {
            return;
        }
        if (this.consumers.countConsumers() == 0) {
            this.tailing.stop();
            this.currentlyProcessedMessage = null;
        }
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
        DefaultLogWatch.LOGGER.info("Terminating {}.", this);
        this.isTerminated = true;
        this.consumers.stop();
        this.handingDown.clear();
        this.sweeping.stop();
        this.previousAcceptedMessage = null;
        DefaultLogWatch.LOGGER.info("Terminated {}.", this);
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("DefaultLogWatch [getUniqueId()=").append(this.getUniqueId()).append(", ");
        if (this.getWatchedFile() != null) {
            builder.append("getWatchedFile()=").append(this.getWatchedFile()).append(", ");
        }
        builder.append("isTerminated()=").append(this.isTerminated()).append("]");
        return builder.toString();
    }

}
