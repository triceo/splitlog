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

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageCondition;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageFormatter;
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
abstract class AbstractLogWatchFollower extends AbstractFollower implements Follower {

    private final Set<Message> tags = new LinkedHashSet<Message>();
    private final DefaultLogWatch watch;
    private final Set<AbstractMergingFollower> mergingFollowersToNotify = new LinkedHashSet<AbstractMergingFollower>();

    protected Set<Message> getTags() {
        return this.tags;
    }

    protected AbstractLogWatchFollower(final DefaultLogWatch watch) {
        this.watch = watch;
    }

    @Override
    public Message tag(final String tagLine) {
        final Message message = new MessageBuilder(tagLine).buildTag();
        this.tags.add(message);
        return message;
    }

    @Override
    protected MessageFormatter getDefaultFormatter() {
        return NoopMessageFormatter.INSTANCE;
    }

    protected Set<AbstractMergingFollower> getMergingFollowersToNotify() {
        return Collections.unmodifiableSet(this.mergingFollowersToNotify);
    }

    @Override
    public LogWatch getFollowed() {
        return this.getWatch();
    }

    protected DefaultLogWatch getWatch() {
        return this.watch;
    }

    protected boolean registerMerge(final AbstractMergingFollower mf) {
        return this.mergingFollowersToNotify.add(mf);
    }

    protected boolean unregisterMerge(final AbstractMergingFollower mf) {
        return this.mergingFollowersToNotify.remove(mf);
    }

    @Override
    public boolean isFollowing() {
        return this.watch.isFollowedBy(this);
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

    @Override
    public boolean write(final OutputStream stream, final MessageCondition condition, final MessageComparator order,
        final MessageFormatter formatter) {
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

    @Override
    public MergingFollower mergeWith(final CommonFollower f) {
        if (f == null) {
            throw new IllegalArgumentException("Cannot merge with null.");
        } else if (f == this) {
            throw new IllegalArgumentException("Cannot merge with self.");
        }
        if (f instanceof MergingFollower) {
            final MergingFollower mf = (MergingFollower) f;
            final Set<Follower> followers = new HashSet<Follower>(mf.getMerged());
            followers.add(this);
            return new NonStoringMergingFollower(followers.toArray(new Follower[followers.size()]));
        } else {
            return new NonStoringMergingFollower(this, f);
        }
    }

    /**
     * Notify the follower of a new message in the watched log. Must never be
     * called by users, just from the library code.
     *
     * Implementors are encouraged to synchronize these operations, to preserve
     * the original order of messages.
     *
     * @param msg
     *            The message.
     * @param status
     *            Status of the message.
     * @param source
     *            Where does the notification come from.
     * @throws IllegalArgumentException
     *             In case the source is a class that should not access to this.
     */
    abstract void notifyOfMessage(Message msg, MessageDeliveryStatus status, LogWatch source);
    
}
