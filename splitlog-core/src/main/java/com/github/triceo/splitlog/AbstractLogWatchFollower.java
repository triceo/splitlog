package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

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
    public MergingFollower mergeWith(final CommonFollower f) {
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
