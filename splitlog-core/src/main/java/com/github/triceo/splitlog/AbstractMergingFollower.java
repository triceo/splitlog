package com.github.triceo.splitlog;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

abstract class AbstractMergingFollower extends AbstractFollower implements MergingFollower {

    private final Set<AbstractFollower> followers = new LinkedHashSet<AbstractFollower>();

    protected AbstractMergingFollower(final Follower... followers) {
        for (final Follower f : followers) {
            if (!(f instanceof AbstractLogWatchFollower)) {
                throw new IllegalArgumentException("Illegal follower: " + f);
            }
            final AbstractLogWatchFollower af = (AbstractLogWatchFollower) f;
            this.followers.add(af);
            af.registerMerge(this);
        }
    }

    @Override
    public boolean separate(final Follower f) {
        if (!this.followers.contains(f)) {
            return false;
        }
        // we know about this follower, so the cast is safe
        final AbstractLogWatchFollower af = (AbstractLogWatchFollower) f;
        return af.unregisterMerge(this);
    }

    @Override
    public boolean isFollowing() {
        for (final Follower f : this.followers) {
            if (f.isFollowing()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<? extends Follower> getMerged() {
        return Collections.unmodifiableSet(this.followers);
    }

    @Override
    public MergingFollower mergeWith(final Follower f) {
        final Set<Follower> followers = new HashSet<Follower>(this.followers);
        if (f instanceof MergingFollower) {
            final MergingFollower mf = (MergingFollower) f;
            followers.addAll(mf.getMerged());
        } else {
            followers.add(f);
        }
        return new NonStoringMergingFollower(followers.toArray(new Follower[followers.size()]));
    }

}
