package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.*;
import com.github.triceo.splitlog.formatters.UnifyingMessageFormatter;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.util.LogUtil;
import com.github.triceo.splitlog.util.LogUtil.Level;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * Will use {@link UnifyingMessageFormatter} as default message formatter.
 *
 */
final class DefaultMergingFollower extends AbstractCommonFollower<MergingFollower, Follower> implements MergingFollower {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(DefaultMergingFollower.class);

    private final ConsumerManager<MergingFollower> consumers = new ConsumerManager<>(this);
    private final Set<Follower> followers = new LinkedHashSet<>();

    protected DefaultMergingFollower(final Follower... followers) {
        DefaultMergingFollower.LOGGER.info("Merging followers into {}.", this);
        final Collection<LogWatch> watches = new HashSet<>();
        for (final Follower f : followers) {
            final DefaultFollower af = (DefaultFollower) f;
            this.followers.add(af);
            af.registerConsumer(this);
            watches.add(af.getFollowed());
        }
        if (watches.size() < this.followers.size()) {
            DefaultMergingFollower.LOGGER.warn("Followers from the same LogWatch, possible message duplication.");
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
    public SortedSet<Message> getMessages(final SimpleMessageCondition condition, final MessageComparator order) {
        final SortedSet<Message> sorted = new TreeSet<>(order);
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
        final Set<Follower> followers = new HashSet<>(this.followers);
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
        final Set<Follower> followers = new HashSet<>(this.followers);
        followers.addAll(f.getMerged());
        return new DefaultMergingFollower(followers.toArray(new Follower[followers.size()]));
    }

    @Override
    public void messageReceived(final Message msg, final MessageDeliveryStatus status, final Follower source) {
        if (this.isStopped()) {
            throw new IllegalStateException("Follower already stopped.");
        } else if (!this.getMerged().contains(source)) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        LogUtil.newMessage(DefaultMergingFollower.LOGGER, Level.INFO, "New message received:", msg, status, source,
                this);
        this.getExpectationManager().messageReceived(msg, status, source);
        this.getConsumerManager().messageReceived(msg, status, this);
    }

    @Override
    public MergingFollower remove(final Follower f) {
        if (!this.getMerged().contains(f)) {
            return this;
        } else if (this.getMerged().size() == 1) {
            return null;
        } else {
            DefaultMergingFollower.LOGGER.info("Separating {} from {}.", f, this);
            final List<Follower> followers = new ArrayList<>(this.followers);
            followers.remove(f);
            return new DefaultMergingFollower(followers.toArray(new Follower[followers.size()]));
        }
    }

    @Override
    public boolean stop() {
        if (this.isStopped()) {
            return false;
        }
        DefaultMergingFollower.LOGGER.info("Stopping {}.", this);
        this.getMerged().forEach(Follower::stop);
        this.getConsumerManager().stop();
        this.getExpectationManager().stop();
        DefaultMergingFollower.LOGGER.info("Stopped {}.", this);
        return true;
    }

    @Override
    public String toString() {
        return "DefaultMergingFollower [getUniqueId()=" + this.getUniqueId() + ", " +
                "getMerged()=" + this.getMerged() + ", " +
                "isStopped()=" + this.isStopped() + "]";
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
        final Collection<Message> messages = new TreeSet<>(order);
        final Map<Message, String> messagesToText = new HashMap<>();
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
