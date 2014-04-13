package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.api.SimpleMessageCondition;

/**
 * This is a log follower that holds no message data, just the tags. For message
 * data, it will always turn to the underlying {@link LogWatch}.
 *
 * This class assumes that LogWatch and user code are the only two threads that
 * use it. Never use one instance of this class from two or more user threads.
 * Otherwise, unpredictable behavior from waitFor() methods is possible.
 *
 * Metrics within will never be terminated (and thus removed) unless done by the
 * user. Not even when no longer {@link #isFollowing()}.
 *
 * FIXME maybe we should do something about that ^^^^
 */
final class NonStoringFollower extends AbstractLogWatchFollower {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringFollower.class);

    private final MessageExchange exchange = new MessageExchange();
    private final MessageMetricManager metrics = new MessageMetricManager();

    public NonStoringFollower(final DefaultLogWatch watch,
        final List<Pair<String, MessageMeasure<?>>> measuresHandedDown) {
        super(watch);
        for (final Pair<String, MessageMeasure<? extends Number>> pair : measuresHandedDown) {
            this.measure(pair.getValue(), pair.getKey(), false);
        }
    }

    @Override
    public SortedSet<Message> getMessages(final SimpleMessageCondition condition, final MessageComparator order) {
        final SortedSet<Message> messages = new TreeSet<Message>(order);
        for (final Message msg : this.getWatch().getAllMessages(this)) {
            if (!condition.accept(msg)) {
                continue;
            }
            messages.add(msg);
        }
        messages.addAll(this.getTags());
        return Collections.unmodifiableSortedSet(messages);
    }

    @Override
    public MessageMetric<? extends Number> getMetric(final String id) {
        return this.metrics.getMetric(id);
    }

    @Override
    public String getMetricId(final MessageMetric<? extends Number> measure) {
        return this.metrics.getMetricId(measure);
    }

    private <T extends Number> MessageMetric<T> measure(final MessageMeasure<T> measure, final String id,
            final boolean checkIfFollowing) {
        if (checkIfFollowing && !this.isFollowing()) {
            throw new IllegalStateException("Cannot start measurement as the follower is no longer active.");
        }
        return this.metrics.startMeasuring(measure, id);
    }

    @Override
    public <T extends Number> MessageMetric<T> startMeasuring(final MessageMeasure<T> measure, final String id) {
        return this.measure(measure, id, true);
    }

    @Override
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status,
        final LogWatch source) {
        if (source != this.getWatch()) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        NonStoringFollower.LOGGER.info("{} notified of '{}' with status {}.", this, msg, status);
        this.exchange.messageReceived(msg, status, source);
        for (final AbstractMergingFollower mf : this.getMergingFollowersToNotify()) {
            mf.messageReceived(msg, status, this);
        }
        this.metrics.messageReceived(msg, status, source);
    }

    @Override
    public boolean stopMeasuring(final MessageMetric<? extends Number> metric) {
        return this.metrics.stopMeasuring(metric);
    }

    @Override
    public boolean stopMeasuring(final String id) {
        return this.metrics.stopMeasuring(id);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MidDeliveryMessageCondition condition) {
        return this.exchange.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MidDeliveryMessageCondition condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.exchange.waitForMessage(condition, timeout, unit);
    }

    @Override
    public boolean isMeasuring(final String id) {
        return this.metrics.isMeasuring(id);
    }

    @Override
    public boolean isMeasuring(final MessageMetric<? extends Number> metric) {
        return this.metrics.isMeasuring(metric);
    }

}