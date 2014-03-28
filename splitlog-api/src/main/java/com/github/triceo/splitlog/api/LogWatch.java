package com.github.triceo.splitlog.api;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

/**
 * The primary point of interaction with this tool. Allows users to start
 * listening to changes in log files.
 */
public interface LogWatch extends MessageDeliveryNotificationSource {

    /**
     * Begin watching for new messages from this point in time.
     * 
     * @return API for watching for messages.
     */
    Follower follow();

    /**
     * Begin watching for new messages from this point in time, immediately
     * calling {@link CommonFollower#waitFor(MessageDeliveryCondition)} - this
     * way, no messages can be missed between the actual start of the tailer and
     * the first wait. .
     * 
     * @param waitFor
     *            Condition to pass to the follower.
     * @return The new follower and the result of the wait call.
     */
    Pair<Follower, Message> follow(MessageDeliveryCondition waitFor);

    /**
     * Begin watching for new messages from this point in time, immediately
     * calling
     * {@link CommonFollower#waitFor(MessageDeliveryCondition, long, TimeUnit)}
     * - this way, no messages can be missed between the actual start of the
     * tailer and the first wait. .
     * 
     * @param waitFor
     *            Condition to pass to the follower.
     * @param howLong
     *            How long to wait for the condition to be met.
     * @param unit
     *            The time unit for the above.
     * @return The new follower and the result of the wait call.
     */
    Pair<Follower, Message> follow(MessageDeliveryCondition waitFor, long howLong, TimeUnit unit);

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
     * Whether or not {@link #terminate()} has been called.
     * 
     * @return True if it has.
     */
    boolean isTerminated();

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
