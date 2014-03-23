package com.github.triceo.splitlog;

import com.github.triceo.splitlog.conditions.MessageCondition;

/**
 * On top of the {@link CommonFollower}'s functions, this allows for merging
 * followers.
 */
public interface Follower extends CommonFollower, MessageDeliveryNotificationSource {

    /**
     * Retrieve the log watch that is being followed.
     * 
     * @return The {@link LogWatch} whose {@link LogWatch#follow()} was called
     *         to obtain reference to this follower.
     */
    public LogWatch getFollowed();

    /**
     * Whether or not this follower is still capable of receiving messages from
     * {@link LogWatch}. It is suggested that the reference to this follower be
     * thrown away immediately after the user has processed the results of
     * {@link #getMessages()} or {@link #getMessages(MessageCondition)}.
     * {@link LogWatch} may then be able to free the memory occupied by those
     * messages.
     * 
     * @return True if following.
     */
    boolean isFollowing();

}
