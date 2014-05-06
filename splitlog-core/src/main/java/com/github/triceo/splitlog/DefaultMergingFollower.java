package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageFormatter;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.formatters.UnifyingMessageFormatter;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

/**
 * Will use {@link UnifyingMessageFormatter} as default message formatter.
 *
 */
final class DefaultMergingFollower extends AbstractCommonFollower<MergingFollower, Follower> implements MergingFollower {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(DefaultMergingFollower.class);

    private final ConsumerManager<MergingFollower> consumers = new ConsumerManager<MergingFollower>(this);
    private final Set<Follower> followers = new LinkedHashSet<Follower>();

    protected DefaultMergingFollower(final Follower... followers) {
        DefaultMergingFollower.LOGGER.info("Merging followers into {}.", this);
        for (final Follower f : followers) {
            final DefaultFollower af = (DefaultFollower) f;
            this.followers.add(af);
            af.registerConsumer(this);
        }
        DefaultMergingFollower.LOGGER.info("Followers merged: {}.", this);
    }

    @Override
    protected ConsumerManager<MergingFollower> getConsumerManager() {
        return this.consumers;
    }

    @Override
    protected MessageFormatter getDefaultFormatter() {
        return UnifyingMessageFormatter.INSTANCE;
    }

    @Override
    public Collection<Follower> getMerged() {
        return Collections.unmodifiableSet(this.followers);
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
    public synchronized boolean isStopped() {
        for (final Follower f : this.followers) {
            if (!f.isStopped()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public MergingFollower mergeWith(final Follower f) {
        if (f == null) {
            throw new IllegalArgumentException("Cannot merge with null.");
        }
        final Set<Follower> followers = new HashSet<Follower>(this.followers);
        followers.add(f);
        return new DefaultMergingFollower(followers.toArray(new Follower[followers.size()]));
    }

    @Override
    public MergingFollower mergeWith(final MergingFollower f) {
        if (f == null) {
            throw new IllegalArgumentException("Cannot merge with null.");
        } else if (f == this) {
            throw new IllegalArgumentException("Cannot merge with self.");
        }
        final Set<Follower> followers = new HashSet<Follower>(this.followers);
        followers.addAll(f.getMerged());
        return new DefaultMergingFollower(followers.toArray(new Follower[followers.size()]));
    }

    @Override
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status,
        final Follower source) {
        if (this.isStopped()) {
            throw new IllegalStateException("Follower already stopped.");
        } else if (!this.getMerged().contains(source)) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        DefaultMergingFollower.LOGGER.info("{} notified of '{}' with status {} by {}.", this, msg, status, source);
        if (this.getExchange() != null) {
            this.getExchange().messageReceived(msg, status, source);
        }
        this.getConsumerManager().messageReceived(msg, status, this);
    }

    @Override
    public boolean separate(final Follower f) {
        if (!this.followers.remove(f)) {
            return false;
        }
        // we know about this follower, so the cast is safe
        DefaultMergingFollower.LOGGER.info("Separating {} from {}.", f, this);
        return f.stopConsuming(this);
    }

    @Override
    public synchronized boolean stop() {
        if (this.isStopped()) {
            return false;
        }
        DefaultMergingFollower.LOGGER.info("Stopping {}.", this);
        for (final Follower f : this.getMerged()) {
            f.stop();
        }
        this.getConsumerManager().stop();
        DefaultMergingFollower.LOGGER.info("Stopped {}.", this);
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("DefaultMergingFollower [getUniqueId()=").append(this.getUniqueId()).append(", ");
        if (this.getMerged() != null) {
            builder.append("getMerged()=").append(this.getMerged()).append(", ");
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
        /*
         * assemble messages per-follower, so that we can properly retrieve
         * their source
         */
        final SortedSet<Message> messages = new TreeSet<Message>(order);
        final Map<Message, String> messagesToText = new HashMap<Message, String>();
        for (final Follower f : this.getMerged()) {
            for (final Message m : f.getMessages(condition)) {
                messages.add(m);
                messagesToText.put(m, formatter.format(m, f.getFollowed().getWatchedFile()));
            }
        }
        // and now write them in their original order
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            for (final Message msg : messages) {
                w.write(messagesToText.get(msg));
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
