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

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageFormatter;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.formatters.UnifyingMessageFormatter;

abstract class AbstractMergingFollower extends AbstractFollower<MergingFollower> implements MergingFollower, MessageConsumer<Follower> {

    private final Set<AbstractLogWatchFollower> followers = new LinkedHashSet<AbstractLogWatchFollower>();

    protected AbstractMergingFollower(final Follower... followers) {
        for (final Follower f : followers) {
            final AbstractLogWatchFollower af = (AbstractLogWatchFollower) f;
            this.followers.add(af);
            af.registerMerge(this);
        }
    }

    @Override
    protected MessageFormatter getDefaultFormatter() {
        return UnifyingMessageFormatter.INSTANCE;
    }

    @Override
    public Collection<? extends Follower> getMerged() {
        return Collections.unmodifiableSet(this.followers);
    }

    @Override
    public boolean isFollowing() {
        for (final AbstractLogWatchFollower f : this.followers) {
            if (f.isFollowing()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MergingFollower mergeWith(final Follower f) {
        if (f == null) {
            throw new IllegalArgumentException("Cannot merge with null.");
        }
        final Set<Follower> followers = new HashSet<Follower>(this.followers);
        followers.add(f);
        return new NonStoringMergingFollower(followers.toArray(new Follower[followers.size()]));
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
        return new NonStoringMergingFollower(followers.toArray(new Follower[followers.size()]));
    }

    @Override
    public boolean separate(final Follower f) {
        if (!this.followers.contains(f)) {
            return false;
        }
        // we know about this follower, so the cast is safe
        this.followers.remove(f);
        final AbstractLogWatchFollower af = (AbstractLogWatchFollower) f;
        return af.unregisterMerge(this);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("AbstractMergingFollower [");
        if (this.followers != null) {
            builder.append("followers=").append(this.followers).append(", ");
        }
        builder.append("isFollowing()=").append(this.isFollowing()).append("]");
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
