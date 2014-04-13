package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageCondition;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageSource;

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

    public NonStoringFollower(final DefaultLogWatch watch) {
        super(watch);
    }

    @Override
    public SortedSet<Message> getMessages(final MessageCondition condition, final MessageComparator order) {
        final SortedSet<Message> messages = new TreeSet<Message>(order);
        for (final Message msg : this.getWatch().getAllMessages(this)) {
            if (!condition.accept(msg, MessageDeliveryStatus.ACCEPTED, this)) {
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

    @Override
    public <T extends Number> MessageMetric<T> measure(final MessageMeasure<T> measure, final String id) {
        if (!this.isFollowing()) {
            throw new IllegalStateException("Cannot start measurement as the follower is no longer active.");
        }
        return this.metrics.measure(measure, id);
    }

    @Override
    synchronized void
    notifyOfMessage(final Message msg, final MessageDeliveryStatus status, final MessageSource source) {
        if (source != this.getWatch()) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        NonStoringFollower.LOGGER.info("{} notified of '{}' with status {}.", this, msg, status);
        this.exchange.notifyOfMessage(msg, status, source);
        for (final AbstractMergingFollower mf : this.getMergingFollowersToNotify()) {
            mf.notifyOfMessage(msg, status, this);
        }
        this.metrics.notifyOfMessage(msg, status, source);
    }

    @Override
    public boolean terminateMeasuring(final MessageMeasure<? extends Number> measure) {
        return this.metrics.terminateMeasuring(measure);
    }

    @Override
    public boolean terminateMeasuring(final String id) {
        return this.metrics.terminateMeasuring(id);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageCondition condition) {
        return this.exchange.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageCondition condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.exchange.waitForMessage(condition, timeout, unit);
    }

}