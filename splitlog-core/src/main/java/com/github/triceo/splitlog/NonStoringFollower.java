package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

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

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(NonStoringFollower.class);

    public NonStoringFollower(final DefaultLogWatch watch,
        final List<Pair<String, MessageMeasure<? extends Number, Follower>>> measuresHandedDown) {
        super(watch);
        for (final Pair<String, MessageMeasure<? extends Number, Follower>> pair : measuresHandedDown) {
            this.startMeasuring(pair.getValue(), pair.getKey(), false);
        }
    }

    // FIXME should be synchronized; but then the tests hang weirdly
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
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status,
        final LogWatch source) {
        if (this.isStopped()) {
            throw new IllegalStateException("Follower already stopped.");
        } else if (source != this.getWatch()) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        NonStoringFollower.LOGGER.info("{} notified of '{}' with status {}.", this, msg, status);
        this.getExchange().messageReceived(msg, status, source);
        this.getConsumerManager().messageReceived(msg, status, this);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("NonStoringFollower [getUniqueId()=").append(this.getUniqueId()).append(", ");
        if (this.getFollowed() != null) {
            builder.append("getFollowed()=").append(this.getFollowed()).append(", ");
        }
        builder.append("isStopped()=").append(this.isStopped()).append("]");
        return builder.toString();
    }

}