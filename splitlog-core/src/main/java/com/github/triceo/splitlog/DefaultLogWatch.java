package com.github.triceo.splitlog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

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

    private final ConsumerManager<LogWatch> consumers = new ConsumerManager<LogWatch>(this);
    private final SimpleMessageCondition gateCondition;
    private final BidiMap<String, MessageMeasure<? extends Number, Follower>> handingDown = new DualHashBidiMap<String, MessageMeasure<? extends Number, Follower>>();
    private boolean isStarted = false;
    private boolean isStopped = false;
    private final LogWatchStorageManager storage;
    private final LogWatchTailingManager tailing;
    private final long uniqueId = DefaultLogWatch.ID_GENERATOR.getAndIncrement();
    private final File watchedFile;

    protected DefaultLogWatch(final LogWatchBuilder builder, final TailSplitter splitter) {
        this.gateCondition = builder.getGateCondition();
        this.storage = new LogWatchStorageManager(this, builder);
        this.watchedFile = builder.getFileToWatch();
        this.tailing = new LogWatchTailingManager(this, builder, splitter);
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

    private boolean hasToLetMessageThroughTheGate(final Message message) {
        if (this.gateCondition.accept(message)) {
            DefaultLogWatch.LOGGER.debug("Message '{}' passed gate condition {} in {}.", message, this.gateCondition,
                    this);
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
    public boolean isFollowedBy(final Follower follower) {
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
    public boolean isStarted() {
        return this.isStarted;
    }

    @Override
    public boolean isStopped() {
        return this.isStopped;
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
    public MessageDeliveryStatus messageArrived(final Message message) {
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
    public boolean messageIncoming(final Message message) {
        if (!this.hasToLetMessageThroughTheGate(message)) {
            return false;
        }
        this.consumers.messageReceived(message, MessageDeliveryStatus.INCOMING, this);
        return true;
    }

    @Override
    public synchronized boolean start() {
        if (this.isStarted()) {
            return false;
        }
        this.isStarted = true;
        this.tailing.start();
        return true;
    }

    @Override
    public synchronized MessageConsumer<LogWatch> startConsuming(final MessageListener<LogWatch> consumer) {
        return this.consumers.startConsuming(consumer);
    }

    @Override
    public Follower startFollowing() {
        return this.startFollowingActually(null).getKey();
    }

    private synchronized Pair<Follower, Future<Message>> startFollowingActually(
            final MidDeliveryMessageCondition<LogWatch> condition) {
        if (this.isStopped()) {
            throw new IllegalStateException("Cannot start following on an already terminated LogWatch.");
        }
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
        return ImmutablePair.of(follower, expectation);
    }

    @Override
    public synchronized boolean startHandingDown(final MessageMeasure<? extends Number, Follower> measure,
        final String id) {
        if (this.isStopped()) {
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

    /**
     * Invoking this method will cause the running message sweep to be
     * de-scheduled. Any currently present {@link Message}s will only be removed
     * from memory when this watch instance is removed from memory.
     */
    @Override
    public synchronized boolean stop() {
        if (!this.isStarted()) {
            throw new IllegalStateException("Cannot terminate what was not started.");
        } else if (this.isStopped()) {
            return false;
        }
        DefaultLogWatch.LOGGER.info("Terminating {}.", this);
        this.isStopped = true;
        this.tailing.stop();
        this.consumers.stop();
        this.handingDown.clear();
        this.storage.logWatchTerminated();
        DefaultLogWatch.LOGGER.info("Terminated {}.", this);
        return true;
    }

    @Override
    public synchronized boolean stopConsuming(final MessageConsumer<LogWatch> consumer) {
        final boolean result = this.consumers.stopConsuming(consumer);
        if (!result) {
            return false;
        }
        if (consumer instanceof Follower) {
            this.storage.followerTerminated((Follower) consumer);
        }
        return true;
    }

    @Override
    public synchronized boolean stopFollowing(final Follower follower) {
        if (!this.isFollowedBy(follower)) {
            return false;
        }
        // handle incomplete message; to be removed in 1.8.x
        final Message current = this.tailing.getCurrentlyProcessedMessage();
        if ((current != null) && this.hasToLetMessageThroughTheGate(current)) {
            follower.messageReceived(current, MessageDeliveryStatus.INCOMPLETE, this);
        }
        // and actually terminate
        this.stopConsuming(follower);
        DefaultLogWatch.LOGGER.info("Unregistered {} for {}.", follower, this);
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

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("DefaultLogWatch [getUniqueId()=").append(this.getUniqueId()).append(", ");
        if (this.getWatchedFile() != null) {
            builder.append("getWatchedFile()=").append(this.getWatchedFile()).append(", ");
        }
        builder.append("isStopped()=").append(this.isStopped()).append("]");
        return builder.toString();
    }

}
