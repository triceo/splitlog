package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageFormatter;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.formatters.NoopMessageFormatter;

/**
 * Internal API for a log follower that, on top of the public API, provides ways
 * for {@link LogWatch} of notifying the follower of new messages. Every
 * follower implementation, such as {@link NonStoringFollower}, needs to extend
 * this class.
 *
 * Will use {@value #DEFAULT_FORMATTER} as default message formatter. Will use
 * {@value #DEFAULT_CONDITION} as a default in getMessages() and write()
 * methods. Will use {@link #DEFAULT_COMPARATOR} as a default order for the
 * messages.
 */
abstract class AbstractLogWatchFollower extends AbstractFollower<Follower> implements Follower {

    private final Set<AbstractMergingFollower> mergingFollowersToNotify = new LinkedHashSet<AbstractMergingFollower>();

    private final Set<Message> tags = new LinkedHashSet<Message>();

    private final DefaultLogWatch watch;

    protected AbstractLogWatchFollower(final DefaultLogWatch watch) {
        this.watch = watch;
    }

    @Override
    protected MessageFormatter getDefaultFormatter() {
        return NoopMessageFormatter.INSTANCE;
    }

    @Override
    public LogWatch getFollowed() {
        return this.getWatch();
    }

    protected Set<AbstractMergingFollower> getMergingFollowersToNotify() {
        return Collections.unmodifiableSet(this.mergingFollowersToNotify);
    }

    protected Set<Message> getTags() {
        return this.tags;
    }

    protected DefaultLogWatch getWatch() {
        return this.watch;
    }

    @Override
    public boolean isFollowing() {
        return this.watch.isFollowedBy(this);
    }

    @Override
    public boolean isStopped() {
        return !this.getFollowed().isFollowedBy(this);
    }

    @Override
    public MergingFollower mergeWith(final Follower f) {
        if (f == null) {
            throw new IllegalArgumentException("Cannot merge with null.");
        } else if (f == this) {
            throw new IllegalArgumentException("Cannot merge with self.");
        }
        return new NonStoringMergingFollower(this, f);
    }

    @Override
    public MergingFollower mergeWith(final MergingFollower f) {
        if (f == null) {
            throw new IllegalArgumentException("Cannot merge with null.");
        }
        final Set<Follower> followers = new HashSet<Follower>(f.getMerged());
        followers.add(this);
        return new NonStoringMergingFollower(followers.toArray(new Follower[followers.size()]));
    }

    protected boolean registerMerge(final AbstractMergingFollower mf) {
        return this.mergingFollowersToNotify.add(mf);
    }

    @Override
    public boolean stop() {
        return this.getFollowed().stopFollowing(this);
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
        builder.append("Follower [");
        if (this.watch != null) {
            builder.append("watch=").append(this.watch).append(", ");
        }
        builder.append("isFollowing()=").append(this.isFollowing()).append("]");
        return builder.toString();
    }

    protected boolean unregisterMerge(final AbstractMergingFollower mf) {
        return this.mergingFollowersToNotify.remove(mf);
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
