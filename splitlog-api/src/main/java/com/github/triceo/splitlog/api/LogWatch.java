package com.github.triceo.splitlog.api;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

/**
 * The primary point of interaction with this tool. Allows users to start
 * listening to changes in log files.
 */
public interface LogWatch extends MessageSource {

    /**
     * Every new {@link Follower} from now on will immediately receive a new
     * {@link MessageMetric} instance with a given ID that is using the given
     * measure instance.
     *
     * @param measure
     *            Measure to use in the newly created {@link MessageMetric}
     *            instance.
     * @param id
     *            The ID to locate the {@link MessageMetric} using
     *            {@link Follower#getMetric(String)}. No relation to the ID used
     *            by {@link #measure(MessageMeasure, String)}.
     * @return False if either the measure or the ID is already being handed
     *         down.
     */
    boolean beHandingDown(final MessageMeasure<? extends Number> measure, final String id);

    /**
     * Begin watching for new messages from this point in time.
     *
     * @return API for watching for messages.
     */
    Follower follow();

    /**
     * Begin watching for new messages from this point in time, immediately
     * calling {@link CommonFollower#waitFor(MidDeliveryMessageCondition)} -
     * this way, no messages can be missed between the actual start of the
     * tailer and the first wait. .
     *
     * @param waitFor
     *            Condition to pass to the follower.
     * @return The new follower and the result of the wait call.
     */
    Pair<Follower, Message> follow(MidDeliveryMessageCondition waitFor);

    /**
     * Begin watching for new messages from this point in time, immediately
     * calling
     * {@link CommonFollower#waitFor(MidDeliveryMessageCondition, long, TimeUnit)}
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
    Pair<Follower, Message> follow(MidDeliveryMessageCondition waitFor, long howLong, TimeUnit unit);

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
     * Invalidate {@link #beHandingDown(MessageMeasure, String)}. No further
     * {@link Follower} will automatically receive {@link MessageMetric} using
     * this measure by default.
     *
     * @param measure
     *            The measure to no longer be handing down to newly instantiated
     *            {@link Follower}s.
     * @return False if it wasn't being handed down.
     */
    boolean stopHandingDown(final MessageMeasure<? extends Number> measure);

    /**
     * Invalidate {@link #beHandingDown(MessageMeasure, String)}. No further
     * {@link Follower} will automatically receive {@link MessageMetric} using
     * this measure by default.
     *
     * @param id
     *            The ID of the {@link MessageMeasure} to no longer be handing
     *            down to newly instantiated {@link Follower}s. No relation to
     *            the ID used by {@link #measure(MessageMeasure, String)}.
     * @return False if it wasn't being handed down.
     */
    boolean stopHandingDown(final String id);

    /**
     * Stop all followers from following and free resources. Will terminate
     * every running measurement via {@link MessageMetric}.
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
