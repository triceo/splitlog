package com.github.triceo.splitlog;

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageFormatter;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.formatters.NoopMessageFormatter;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.util.LogUtil;
import com.github.triceo.splitlog.util.LogUtil.Level;

/**
 * This is a log follower that holds no message data, just the tags. For message
 * data, it will always turn to the underlying {@link LogWatch}.
 *
 * This class assumes that LogWatch and user code are the only two threads that
 * use it. Never use one instance of this class from two or more user threads.
 * Otherwise, unpredictable behavior from waitFor() methods is possible.
 *
 * Will use {@link NoopMessageFormatter} as default message formatter.
 */
final class DefaultFollower extends AbstractCommonFollower<Follower, LogWatch> implements Follower {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(DefaultFollower.class);

    private final ConsumerManager<Follower> consumers = new ConsumerManager<>(this);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final SortedSet<Message> tags = new TreeSet<>();

    private final DefaultLogWatch watch;

    public DefaultFollower(final DefaultLogWatch watch,
        final List<Pair<String, MessageMeasure<? extends Number, Follower>>> measuresHandedDown) {
        for (final Pair<String, MessageMeasure<? extends Number, Follower>> pair : measuresHandedDown) {
            this.startMeasuring(pair.getValue(), pair.getKey());
        }
        this.watch = watch;
    }

    @Override
    protected ConsumerManager<Follower> getConsumerManager() {
        return this.consumers;
    }

    @Override
    protected MessageFormatter getDefaultFormatter() {
        return NoopMessageFormatter.INSTANCE;
    }

    @Override
    public LogWatch getFollowed() {
        return this.getWatch();
    }

    @Override
    public SortedSet<Message> getMessages(final SimpleMessageCondition condition,
            final MessageComparator order) {
        final ObjectSortedSet<Message> messages = new ObjectRBTreeSet<>(order);
        for (final Message msg : this.getWatch().getAllMessages(this)) {
            if (!condition.accept(msg)) {
                continue;
            }
            messages.add(msg);
        }
        messages.addAll(this.tags);
        return Collections.unmodifiableSortedSet(messages);
    }

    protected DefaultLogWatch getWatch() {
        return this.watch;
    }

    @Override
    public synchronized boolean isStopped() {
        if (this.isStopped.get()) {
            return true;
        } else if (this.getFollowed().isFollowedBy(this)) {
            return false;
        } else {
            this.isStopped.set(true);
            return true;
        }
    }

    @Override
    public MergingFollower mergeWith(final Follower f) {
        if (f == null) {
            throw new IllegalArgumentException("Cannot merge with null.");
        } else if (f == this) {
            throw new IllegalArgumentException("Cannot merge with self.");
        }
        return new DefaultMergingFollower(this, f);
    }

    @Override
    public MergingFollower mergeWith(final MergingFollower f) {
        if (f == null) {
            throw new IllegalArgumentException("Cannot merge with null.");
        }
        final Set<Follower> followers = new HashSet<>(f.getMerged());
        followers.add(this);
        return new DefaultMergingFollower(followers.toArray(new Follower[followers.size()]));
    }

    @Override
    public void messageReceived(final Message msg, final MessageDeliveryStatus status,
        final LogWatch source) {
        if (this.isStopped()) {
            throw new IllegalStateException("Follower already stopped.");
        } else if (source != this.getWatch()) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        LogUtil.newMessage(DefaultFollower.LOGGER, Level.INFO, "New message received:", msg, status, source, this);
        this.getExpectationManager().messageReceived(msg, status, source);
        this.getConsumerManager().messageReceived(msg, status, this);
    }

    @Override
    public boolean stop() {
        if (this.isStopped()) {
            return false;
        }
        DefaultFollower.LOGGER.info("Stopping {}.", this);
        this.getFollowed().stopFollowing(this);
        this.getConsumerManager().stop();
        this.getExpectationManager().stop();
        DefaultFollower.LOGGER.info("Stopped {}.", this);
        return true;
    }

    @Override
    public Message tag(final String tagLine) {
        final Message message = new MessageBuilder(tagLine).buildTag();
        this.tags.add(message);
        return message;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("DefaultFollower [getUniqueId()=").append(this.getUniqueId()).append(", ");
        if (this.getFollowed() != null) {
            builder.append("getFollowed()=").append(this.getFollowed()).append(", ");
        }
        builder.append("isStopped()=").append(this.isStopped()).append("]");
        return builder.toString();
    }

    @Override
    public boolean write(final OutputStream stream, final SimpleMessageCondition condition,
        final MessageComparator order, final MessageFormatter formatter) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream may not be null.");
        } else if (condition == null) {
            throw new IllegalArgumentException("Condition may not be null.");
        } else if (order == null) {
            throw new IllegalArgumentException("Comparator may not be null.");
        }
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            for (final Message msg : this.getMessages(condition, order)) {
                w.write(formatter.format(msg, this.getFollowed().getWatchedFile()));
                w.newLine();
            }
            return true;
        } catch (final IOException ex) {
            return false;
        } finally {
            IOUtils.closeQuietly(w);
        }
    }

}
