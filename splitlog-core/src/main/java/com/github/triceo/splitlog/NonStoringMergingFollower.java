package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.api.SimpleMessageCondition;

final class NonStoringMergingFollower extends AbstractMergingFollower {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringFollower.class);

    private final ConsumerManager<MergingFollower> consumers = new ConsumerManager<MergingFollower>(this);
    private final MessageExchange<Follower> exchange = new MessageExchange<Follower>();

    public NonStoringMergingFollower(final Follower... followers) {
        super(followers);
    }

    @Override
    public SortedSet<Message> getMessages(final SimpleMessageCondition condition, final MessageComparator order) {
        final SortedSet<Message> sorted = new TreeSet<Message>(order);
        for (final Follower f : this.getMerged()) {
            for (final Message m : f.getMessages()) {
                if (!condition.accept(m)) {
                    continue;
                }
                sorted.add(m);
            }
        }
        return Collections.unmodifiableSortedSet(sorted);
    }

    @Override
    public boolean isConsuming(final MessageConsumer<MergingFollower> consumer) {
        return this.consumers.isConsuming(consumer);
    }

    @Override
    public void messageReceived(final Message msg, final MessageDeliveryStatus status, final Follower source) {
        if (!this.getMerged().contains(source)) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        NonStoringMergingFollower.LOGGER.info("{} notified of '{}' with status {} by {}.", this, msg, status, source);
        this.exchange.messageReceived(msg, status, source);
    }

    @Override
    public boolean startConsuming(final MessageConsumer<MergingFollower> consumer) {
        return this.consumers.startConsuming(consumer);
    }

    @Override
    public boolean stopConsuming(final MessageConsumer<MergingFollower> consumer) {
        return this.consumers.stopConsuming(consumer);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MidDeliveryMessageCondition<Follower> condition) {
        return this.exchange.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MidDeliveryMessageCondition<Follower> condition, final long timeout,
        final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.exchange.waitForMessage(condition, timeout, unit);
    }

}
