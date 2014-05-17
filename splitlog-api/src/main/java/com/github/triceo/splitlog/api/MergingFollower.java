package com.github.triceo.splitlog.api;

import java.util.Collection;

/**
 * Follower that is capable of merging multiple {@link Follower}s.
 *
 * They receive all messages that their {@link #getMerged()} receive. If more
 * followers receive the same message, this receive all of them.
 *
 * Unlike {@link Follower}, this one can not tag. However, it will retrieve
 * every tag that has been made using any of the {@link #getMerged()}.
 *
 */
public interface MergingFollower extends CommonFollower<MergingFollower, Follower> {

    /**
     * Retrieve followers that are currently part of this merge.
     *
     * @return Unmodifiable collections of followers in this merge.
     */
    Collection<Follower> getMerged();

    /**
     * Will return an instance whose {@link #getMerged()} does not contain the
     * given follower.
     *
     * @param f
     *            The follower to remove from the merge.
     * @return New instance of the follower containing the followers without
     *         this one, if {@link #getMerged()} contained it. If it didn't, the
     *         same instance is returned. Null is returned when the merge would
     *         be empty after this call.
     */
    MergingFollower remove(Follower f);

}
