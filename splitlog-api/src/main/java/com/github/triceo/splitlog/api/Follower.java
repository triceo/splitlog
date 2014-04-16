package com.github.triceo.splitlog.api;

/**
 * On top of the {@link CommonFollower}'s functions, this allows for merging
 * followers.
 */
public interface Follower extends CommonFollower<Follower, LogWatch>, MessageMetricProducer<Follower> {

    /**
     * Retrieve the log watch that is being followed.
     *
     * @return The {@link LogWatch} whose {@link LogWatch#startFollowing()} was
     *         called to obtain reference to this follower.
     */
    LogWatch getFollowed();

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
     * @throws IllegalStateException
     *             When already {@link #isStopped()}.
     */
    Message tag(String tagLine);

}
