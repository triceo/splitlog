package com.github.triceo.splitlog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
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

    private final TailSplitter splitter;
    private boolean isTerminated = false;
    private final Set<AbstractLogWatchFollower> followers = new LinkedHashSet<AbstractLogWatchFollower>();
    private final File watchedFile;
    private final LogWatchTailingManager tailing;
    private final LogWatchSweepingManager sweeping;
    private final LogWatchStorageManager messaging;
    private final MessageMetricManager metrics = new MessageMetricManager();
    private final BidiMap<String, MessageMeasure<? extends Number>> handingDown = new DualHashBidiMap<String, MessageMeasure<? extends Number>>();
    private MessageBuilder currentlyProcessedMessage;
    private WeakReference<Message> previousAcceptedMessage;

    protected DefaultLogWatch(final File watchedFile, final TailSplitter splitter, final int capacity,
            final SimpleMessageCondition acceptanceCondition, final long delayBetweenReads,
        final long delayBetweenSweeps, final boolean ignoreExistingContent, final boolean reopenBetweenReads,
        final int bufferSize, final long delayForTailerStart) {
        this.splitter = splitter;
        this.messaging = new LogWatchStorageManager(this, capacity, acceptanceCondition);
        this.tailing = new LogWatchTailingManager(this, delayBetweenReads, delayForTailerStart, ignoreExistingContent,
                reopenBetweenReads, bufferSize);
        this.sweeping = new LogWatchSweepingManager(this.messaging, delayBetweenSweeps);
        this.watchedFile = watchedFile;
    }

    /**
     * <strong>This is not part of the public API.</strong> Purely for purposes
     * of testing the automated message sweep.
     *
     * @return How many messages there currently are in the internal message
     *         store.
     */
    int countMessagesInStorage() {
        return this.messaging.getMessageStore().size();
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
        for (final AbstractLogWatchFollower f : this.followers) {
            f.messageReceived(message, MessageDeliveryStatus.INCOMING, this);
        }
        this.metrics.messageReceived(message, MessageDeliveryStatus.INCOMING, this);
        return message;
    }

    /**
     * Notify {@link Follower} of a message that could not be delivered fully as
     * the Follower terminated.
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
        this.metrics.messageReceived(message, MessageDeliveryStatus.INCOMPLETE, this);
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
        final boolean messageAccepted = this.messaging.registerMessage(message, this);
        final MessageDeliveryStatus status = messageAccepted ? MessageDeliveryStatus.ACCEPTED
                : MessageDeliveryStatus.REJECTED;
        for (final AbstractLogWatchFollower f : this.followers) {
            f.messageReceived(message, status, this);
        }
        this.metrics.messageReceived(message, status, this);
        return messageAccepted ? message : null;
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
        return this.messaging.getAllMessages(follower);
    }

    @Override
    public synchronized boolean isTerminated() {
        return this.isTerminated;
    }

    @Override
    public synchronized boolean isFollowedBy(final Follower follower) {
        return this.followers.contains(follower);
    }

    @Override
    public File getWatchedFile() {
        return this.watchedFile;
    }

    @Override
    public Follower startFollowing() {
        final Follower f = this.startFollowingActually(false);
        this.tailing.waitUntilStarted();
        return f;
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
        // assemble list of metrics to be handing down and then the follower
        final List<Pair<String, MessageMeasure<? extends Number>>> pairs = new ArrayList<Pair<String, MessageMeasure<? extends Number>>>();
        for (final BidiMap.Entry<String, MessageMeasure<? extends Number>> entry : this.handingDown.entrySet()) {
            pairs.add(ImmutablePair.<String, MessageMeasure<? extends Number>> of(entry.getKey(), entry.getValue()));
        }
        final AbstractLogWatchFollower follower = new NonStoringFollower(this, pairs);
        // register the follower
        this.followers.add(follower);
        this.messaging.followerStarted(follower);
        DefaultLogWatch.LOGGER.info("Registered {} for {}.", follower, this);
        if (this.followers.size() == 1) {
            // we have listeners, let's start tailing
            this.tailing.start(needsToWait);
        }
        return follower;
    }

    @Override
    public Pair<Follower, Message> startFollowing(final MidDeliveryMessageCondition waitFor) {
        final Follower f = this.startFollowingActually(true);
        return ImmutablePair.of(f, f.waitFor(waitFor));
    }

    @Override
    public Pair<Follower, Message> startFollowing(final MidDeliveryMessageCondition waitFor, final long howLong,
            final TimeUnit unit) {
        final Follower f = this.startFollowingActually(true);
        return ImmutablePair.of(f, f.waitFor(waitFor, howLong, unit));
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
        /*
         * methods within the loop will remove from the original list; this
         * copying prevents CMEs.
         */
        final List<AbstractLogWatchFollower> copyOfFollowers = new ArrayList<AbstractLogWatchFollower>(this.followers);
        for (final AbstractLogWatchFollower chunk : copyOfFollowers) {
            this.stopFollowing(chunk);
        }
        this.metrics.terminateMeasuring();
        this.handingDown.clear();
        this.sweeping.stop();
        this.previousAcceptedMessage = null;
        return true;
    }

    @Override
    public synchronized boolean stopFollowing(final Follower follower) {
        if (!this.followers.remove(follower)) {
            return false;
        }
        this.messaging.followerTerminated(follower);
        DefaultLogWatch.LOGGER.info("Unregistered {} for {}.", follower, this);
        if (this.currentlyProcessedMessage != null) {
            this.handleUndeliveredMessage((AbstractLogWatchFollower) follower, this.currentlyProcessedMessage);
        }
        if (this.followers.size() == 0) {
            this.tailing.stop();
            this.currentlyProcessedMessage = null;
        }
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

    @Override
    public <T extends Number> MessageMetric<T> startMeasuring(final MessageMeasure<T> measure, final String id) {
        if (!this.isTerminated()) {
            throw new IllegalStateException("Cannot start measuring, log watch already terminated.");
        }
        return this.metrics.startMeasuring(measure, id);
    }

    @Override
    public MessageMetric<? extends Number> getMetric(final String id) {
        return this.metrics.getMetric(id);
    }

    @Override
    public String getMetricId(final MessageMetric<? extends Number> measure) {
        return this.metrics.getMetricId(measure);
    }

    @Override
    public boolean stopMeasuring(final String id) {
        return this.metrics.stopMeasuring(id);
    }

    @Override
    public boolean stopMeasuring(final MessageMetric<? extends Number> metric) {
        return this.metrics.stopMeasuring(metric);
    }

    @Override
    public synchronized boolean startHandingDown(final MessageMeasure<? extends Number> measure, final String id) {
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
    public synchronized boolean stopHandingDown(final MessageMeasure<? extends Number> measure) {
        return (this.handingDown.removeValue(measure) != null);
    }

    @Override
    public synchronized boolean stopHandingDown(final String id) {
        return (this.handingDown.remove(id) != null);
    }

    @Override
    public boolean isMeasuring(final String id) {
        return this.metrics.isMeasuring(id);
    }

    @Override
    public boolean isMeasuring(final MessageMetric<? extends Number> metric) {
        return this.metrics.isMeasuring(metric);
    }

    @Override
    public boolean isHandingDown(final MessageMeasure<? extends Number> measure) {
        return this.handingDown.containsValue(measure);
    }

    @Override
    public boolean isHandingDown(final String id) {
        return this.handingDown.containsKey(id);
    }

}
