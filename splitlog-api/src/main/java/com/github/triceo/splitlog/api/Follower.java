package com.github.triceo.splitlog.api;

import java.util.concurrent.TimeUnit;

/**
 * On top of the {@link CommonFollower}'s functions, this allows for merging
 * followers.
 */
public interface Follower extends CommonFollower<Follower>, MessageMetricProducer<Follower> {

    /**
     * Retrieve the log watch that is being followed.
     *
     * @return The {@link LogWatch} whose {@link LogWatch#startFollowing()} was
     *         called to obtain reference to this follower.
     */
    LogWatch getFollowed();

    /**
     * Whether or not this follower is still capable of receiving messages from
     * {@link LogWatch}. It is suggested that the reference to this follower be
     * thrown away immediately after the user has processed the results of
     * {@link #getMessages()} or {@link #getMessages(SimpleMessageCondition)}.
     * {@link LogWatch} may then be able to free the memory occupied by those
     * messages.
     *
     * @return True if following.
     */
    boolean isFollowing();

    /**
     * Mark the current location in the tail by a custom message.
     *
     * In case the messages before and after the tag should be discarded in the
     * future, the tag should still remain in place - this will give users the
     * notification that some messages had been discarded.
     *
     * Please note that the current location is indicated by messages that are
     * {@link MessageDeliveryStatus#INCOMING}. If a tag is placed after such
     * message is created and the message only becomes
     * {@link MessageDeliveryStatus#ACCEPTED} later, the tag will still follow.
     *
     * @param tagLine
     *            Text of the message.
     * @return The tag message that was recorded.
     */
    Message tag(String tagLine);

    /**
     * Will block until a message arrives, for which the condition is true.
     *
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Null if the method unblocked due to some other reason.
     */
    Message waitFor(MidDeliveryMessageCondition<LogWatch> condition);

    /**
     * Will block until a message arrives, for which the condition is true. If
     * none arrives before the timeout, it unblocks anyway.
     *
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @param timeout
     *            Time before forcibly aborting.
     * @param unit
     *            Unit of time.
     * @return Null if the method unblocked due to some other reason.
     */
    Message waitFor(MidDeliveryMessageCondition<LogWatch> condition, long timeout, TimeUnit unit);

}
