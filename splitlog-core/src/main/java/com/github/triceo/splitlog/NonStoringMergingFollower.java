package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

final class NonStoringMergingFollower extends AbstractMergingFollower {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(NonStoringFollower.class);

    public NonStoringMergingFollower(final Follower... followers) {
        super(followers);
    }

    @Override
    public synchronized SortedSet<Message> getMessages(final SimpleMessageCondition condition,
            final MessageComparator order) {
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
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status,
        final Follower source) {
        if (this.isStopped()) {
            throw new IllegalStateException("Follower already stopped.");
        } else if (!this.getMerged().contains(source)) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        NonStoringMergingFollower.LOGGER.info("{} notified of '{}' with status {} by {}.", this, msg, status, source);
        this.getExchange().messageReceived(msg, status, source);
        this.getConsumerManager().messageReceived(msg, status, this);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("NonStoringMergingFollower [getUniqueId()=").append(this.getUniqueId()).append(", ");
        if (this.getMerged() != null) {
            builder.append("getMerged()=").append(this.getMerged()).append(", ");
        }
        builder.append("isStopped()=").append(this.isStopped()).append("]");
        return builder.toString();
    }

}
