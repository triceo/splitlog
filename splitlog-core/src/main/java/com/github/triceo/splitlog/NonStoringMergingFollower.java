package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.SimpleMessageCondition;

final class NonStoringMergingFollower extends AbstractMergingFollower {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringFollower.class);

    private final ConsumerManager<MergingFollower> consumers = new ConsumerManager<MergingFollower>(this);

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
        if (this.isStopped()) {
            throw new IllegalStateException("Follower already stopped.");
        } else if (!this.getMerged().contains(source)) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        NonStoringMergingFollower.LOGGER.info("{} notified of '{}' with status {} by {}.", this, msg, status, source);
        this.getExchange().messageReceived(msg, status, source);
    }

    @Override
    public MessageConsumer<MergingFollower> startConsuming(final MessageListener<MergingFollower> consumer) {
        return this.consumers.startConsuming(consumer);
    }

    @Override
    public boolean stopConsuming(final MessageConsumer<MergingFollower> consumer) {
        return this.consumers.stopConsuming(consumer);
    }

}
