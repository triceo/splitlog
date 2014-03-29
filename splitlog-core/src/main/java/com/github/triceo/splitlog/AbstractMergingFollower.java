package com.github.triceo.splitlog;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.MessageFormatter;
import com.github.triceo.splitlog.formatters.UnifyingMessageFormatter;

abstract class AbstractMergingFollower extends AbstractFollower implements MergingFollower {

    private final Set<AbstractLogWatchFollower> followers = new LinkedHashSet<AbstractLogWatchFollower>();

    protected AbstractMergingFollower(final CommonFollower... followers) {
        for (final CommonFollower f : followers) {
            if (!(f instanceof AbstractLogWatchFollower)) {
                throw new IllegalArgumentException("Illegal follower: " + f);
            }
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
    public boolean isFollowing() {
        for (final AbstractLogWatchFollower f : this.followers) {
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
    public String toString() {
        // properly size the builder
        final String merges = this.getMerged().toString();
        final int length = 35 + merges.length();
        final StringBuilder builder = new StringBuilder(length);
        // build
        builder.append("Follower [merges=");
        builder.append(merges);
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
        final Set<CommonFollower> followers = new HashSet<CommonFollower>(this.followers);
        if (f instanceof MergingFollower) {
            final MergingFollower mf = (MergingFollower) f;
            followers.addAll(mf.getMerged());
        } else {
            followers.add(f);
        }
        return new NonStoringMergingFollower(followers.toArray(new Follower[followers.size()]));
    }

}
