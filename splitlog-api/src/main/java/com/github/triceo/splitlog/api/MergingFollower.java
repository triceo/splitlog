package com.github.triceo.splitlog.api;

import java.util.Collection;

/**
 * Follower that is capable of merging multiple {@link Follower}s.
 *
 * Merged followers must obey the following rules:
 *
 * <ul>
 * <li>They receive all messages that their {@link #getMerged()} receive. It is
 * left to the discretion of users to {@link #waitFor(MessageDeliveryCondition)}
 * any message or just for messages from a particular {@link MessageSource}.</li>
 * <li>Their {@link #tag(String)} doesn't affect any tags in their
 * {@link #getMerged()}. They will always be added on top and not propagated.</li>
 * </ul>
 *
 * Unlike {@link Follower}, this one can not tag. However, it will retrieve every
 * tag that has been made using any of the {@link #getMerged()}.
 *
 */
public interface MergingFollower extends CommonFollower {

    /**
     * Retrieve followers that are currently part of this merge.
     *
     * @return Unmodifiable collections of followers in this merge.
     */
    Collection<? extends Follower> getMerged();

    /**
     * Whether or not this follower is still capable of receiving any new
     * messages.
     *
     * @return True if any of {@link #getMerged()}'s {@link #isFollowing()} is
     *         true.
     */
    boolean isFollowing();

    /**
     * Will remove the follower from {@link #getMerged()}. As a result, this
     * merge will act as if it never knew of this follower.
     *
     * It is the responsibility of this method to notify the {@link Follower} to
     * no longer notify of new messages.
     *
     * @param f
     *            The follower to remove from the merge.
     * @return True if the follower was part of the merge, false if it was
     *         already separate or never merged.
     */
    boolean separate(Follower f);

}
