package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.MessageDeliveryNotificationSource;
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
abstract class AbstractLogWatchFollower extends AbstractFollower implements MessageDeliveryNotificationSource, Follower {

    private final DefaultLogWatch watch;
    private final Set<AbstractMergingFollower> mergingFollowersToNotify = new LinkedHashSet<AbstractMergingFollower>();

    protected AbstractLogWatchFollower(final DefaultLogWatch watch) {
        this.watch = watch;
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
        // properly size the builder
        final String watch = this.watch.toString();
        final int length = 35 + watch.length();
        final StringBuilder builder = new StringBuilder(length);
        // build
        builder.append("Follower [watch=");
        builder.append(watch);
        if (this.isFollowing()) {
            builder.append(", following");
        } else {
            builder.append(", not following");
        }
        builder.append(']');
        return builder.toString();
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

}
