package com.github.triceo.splitlog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageCondition;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
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
    private MessageBuilder currentlyProcessedMessage;
    private WeakReference<Message> previousAcceptedMessage;

    protected DefaultLogWatch(final File watchedFile, final TailSplitter splitter, final int capacity,
            final MessageCondition acceptanceCondition, final long delayBetweenReads, final long delayBetweenSweeps,
            final boolean ignoreExistingContent, final boolean reopenBetweenReads, final int bufferSize,
            final long delayForTailerStart) {
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
        for (final AbstractFollower f : this.followers) {
            f.notifyOfMessage(message, MessageDeliveryStatus.INCOMING, this);
        }
        this.metrics.notifyOfMessage(message, MessageDeliveryStatus.INCOMING, this);
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
    private synchronized Message handleUndeliveredMessage(final AbstractFollower follower,
        final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildIntermediate(this.splitter);
        follower.notifyOfMessage(message, MessageDeliveryStatus.UNDELIVERED, this);
        this.metrics.notifyOfMessage(message, MessageDeliveryStatus.UNDELIVERED, this);
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
        for (final AbstractFollower f : this.followers) {
            f.notifyOfMessage(message, status, this);
        }
        this.metrics.notifyOfMessage(message, status, this);
        return messageAccepted ? message : null;
    }

    /**
     * Return all messages that have been sent to a given {@link Follower}, from
     * its {@link #follow()} until either its {@link #unfollow(Follower)} or to
     * this moment, whichever is relevant.
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
    public Follower follow() {
        final Follower f = this.followInternal(false);
        this.tailing.waitUntilStarted();
        return f;
    }

    /**
     * First invocation of the method on an instance will trigger
     * {@link LogWatchStorageSweeper} to be scheduled for periodical sweeping of
     * unreachable messages.
     *
     * @param boolean If the tailer needs a delayed start because of
     *        {@link #follow(MessageCondition)}, as explained in
     *        {@link LogWatchBuilder#getDelayBeforeTailingStarts()}.
     * @return The follower that follows this log watch from now on.
     */
    private synchronized Follower followInternal(final boolean needsToWait) {
        if (this.isTerminated()) {
            throw new IllegalStateException("Cannot start tailing on an already terminated LogWatch.");
        }
        this.sweeping.start();
        final AbstractLogWatchFollower follower = new NonStoringFollower(this);
        this.followers.add(follower);
        this.messaging.followerStarted(follower);
        DefaultLogWatch.LOGGER.info("Registered {} for {}.", follower, this);
        if (this.followers.size() == 1) {
            this.tailing.start(needsToWait);
        }
        return follower;
    }

    @Override
    public Pair<Follower, Message> follow(final MessageCondition waitFor) {
        final Follower f = this.followInternal(true);
        return ImmutablePair.of(f, f.waitFor(waitFor));
    }

    @Override
    public Pair<Follower, Message> follow(final MessageCondition waitFor, final long howLong, final TimeUnit unit) {
        final Follower f = this.followInternal(true);
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
            this.unfollow(chunk);
        }
        this.sweeping.stop();
        this.previousAcceptedMessage = null;
        return true;
    }

    @Override
    public synchronized boolean unfollow(final Follower follower) {
        if (!this.followers.remove(follower)) {
            return false;
        }
        this.messaging.followerTerminated(follower);
        DefaultLogWatch.LOGGER.info("Unregistered {} for {}.", follower, this);
        if (this.currentlyProcessedMessage != null) {
            this.handleUndeliveredMessage((AbstractFollower) follower, this.currentlyProcessedMessage);
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
    public <T extends Number> MessageMetric<T> measure(final MessageMeasure<T> measure, final String id) {
        return this.metrics.measure(measure, id);
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
    public boolean terminateMeasuring(final String id) {
        return this.metrics.terminateMeasuring(id);
    }

    @Override
    public boolean terminateMeasuring(final MessageMeasure<? extends Number> measure) {
        return this.metrics.terminateMeasuring(measure);
    }

}
