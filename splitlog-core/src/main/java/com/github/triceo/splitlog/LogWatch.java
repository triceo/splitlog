package com.github.triceo.splitlog;

import java.io.File;

/**
 * The primary point of interaction with this tool. Allows users to start
 * listening to changes in log files. Use {@link LogWatchBuilder} to get an
 * instance.
 */
public interface LogWatch extends MessageDeliveryNotificationSource {

    /**
     * Whether or not {@link #terminate()} has been called.
     * 
     * @return True if it has.
     */
    boolean isTerminated();

    /**
     * The file that is being tracked by this class.
     * 
     * @return Never null
     */
    File getWatchedFile();

    /**
     * Whether or not {@link #unfollow(Follower)} has been called for a given
     * follower.
     * 
     * @param follower
     *            Tailer in question.
     * @return True if it has.
     */
    boolean isFollowedBy(final Follower follower);

    /**
     * Begin watching for new messages from this point in time.
     * 
     * @return API for watching for messages.
     */
    Follower follow();

    /**
     * Stop all followers from following and free resources.
     * 
     * @return True if terminated as a result, false if already terminated.
     */
    boolean terminate();

    /**
     * Stop particular follower from following.
     * 
     * @param follower
     *            This follower will receive no more messages.
     * @return True if terminated as a result, false if already terminated.
     */
    boolean unfollow(final Follower follower);
}
