package com.github.triceo.splitlog.api;

import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

/**
 * The primary point of interaction with this tool. Allows users to start
 * listening to changes in log files.
 */
public interface LogWatch extends MessageProducer<LogWatch> {

    /**
     * The file that is being tracked by this class.
     *
     * @return Never null
     */
    File getWatchedFile();

    /**
     * Whether or not {@link #stopFollowing(Follower)} has been called for a
     * given follower.
     *
     * @param follower
     *            Tailer in question.
     * @return True if it has.
     */
    boolean isFollowedBy(final Follower follower);

    /**
     * Whether or not particular {@link MessageMeasure} is being automatically
     * handed down to new {@link Follower}s.
     *
     * @param measure
     *            Measure in question.
     * @return True after {@link #startHandingDown(MessageMeasure, String)} has
     *         been called and before {@link #stopHandingDown(MessageMeasure)}.
     */
    boolean isHandingDown(final MessageMeasure<? extends Number, Follower> measure);

    /**
     * Whether or not particular {@link MessageMeasure} is being automatically
     * handed down to new {@link Follower}s.
     *
     * @param id
     *            ID in question.
     * @return True after {@link #startHandingDown(MessageMeasure, String)} has
     *         been called and before {@link #stopHandingDown(String)}.
     */
    boolean isHandingDown(final String id);

    /**
     * Whether or not {@link #start()} has been called.
     * @return
     */
    boolean isStarted();

    /**
     * Whether or not {@link #terminate()} has been called.
     *
     * @return True if it has.
     */
    boolean isTerminated();

    /**
     * Start tailing the log file.
     * @return True if just started, false if already started.
     */
    boolean start();

    /**
     * Begin watching for new messages from this point in time.
     *
     * @return API for watching for messages.
     */
    Follower startFollowing();

    /**
     * Begin watching for new messages from this point in time, immediately
     * calling {@link Follower#waitFor(MidDeliveryMessageCondition)} - this way,
     * no messages can be missed between the actual start of the tailer and the
     * first wait. .
     *
     * In version 1.7.0, this function will change the return value to that of
     * {@link #startFollowingWithExpectation(MidDeliveryMessageCondition)}.
     *
     * @param waitFor
     *            Condition to pass to the follower.
     * @return The new follower and the result of the wait call.
     */
    @Deprecated
    Pair<Follower, Message> startFollowing(MidDeliveryMessageCondition<LogWatch> waitFor);

    /**
     * Begin watching for new messages from this point in time, immediately
     * calling
     * {@link Follower#waitFor(MidDeliveryMessageCondition, long, TimeUnit)} -
     * this way, no messages can be missed between the actual start of the
     * tailer and the first wait.
     *
     * In version 1.7.0, this function will disappear entirely.
     * {@link #startFollowingWithExpectation(MidDeliveryMessageCondition)} will
     * provide all the functionality of this method.
     *
     * @param waitFor
     *            Condition to pass to the follower.
     * @param howLong
     *            How long to wait for the condition to be met.
     * @param unit
     *            The time unit for the above.
     * @return The new follower and the result of the wait call.
     */
    @Deprecated
    Pair<Follower, Message> startFollowing(MidDeliveryMessageCondition<LogWatch> waitFor, long howLong, TimeUnit unit);

    /**
     * Begin watching for new messages from this point in time, immediately
     * calling {@link Follower#expect(MidDeliveryMessageCondition)} - this way,
     * no messages can be missed between the actual start of the tailer and the
     * first wait.
     *
     * In 1.7.0 version, this function will be renamed to
     * {@link #startFollowing(MidDeliveryMessageCondition)}.
     *
     * @param waitFor
     *            Condition to pass to the follower.
     * @return The new follower and the expectation.
     */
    @Deprecated
    Pair<Follower, Future<Message>> startFollowingWithExpectation(MidDeliveryMessageCondition<LogWatch> waitFor);

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
     *            by {@link #startMeasuring(MessageMeasure, String)}.
     * @return False if either the measure or the ID is already being handed
     *         down.
     */
    boolean startHandingDown(final MessageMeasure<? extends Number, Follower> measure, final String id);

    /**
     * Stop particular follower from following.
     *
     * @param follower
     *            This follower will receive no more messages.
     * @return True if terminated as a result, false if already terminated.
     */
    boolean stopFollowing(final Follower follower);

    /**
     * Invalidate {@link #startHandingDown(MessageMeasure, String)}. No further
     * {@link Follower} will automatically receive {@link MessageMetric} using
     * this measure by default.
     *
     * @param measure
     *            The measure to no longer be handing down to newly instantiated
     *            {@link Follower}s.
     * @return False if it wasn't being handed down.
     */
    boolean stopHandingDown(final MessageMeasure<? extends Number, Follower> measure);

    /**
     * Invalidate {@link #startHandingDown(MessageMeasure, String)}. No further
     * {@link Follower} will automatically receive {@link MessageMetric} using
     * this measure by default.
     *
     * @param id
     *            The ID of the {@link MessageMeasure} to no longer be handing
     *            down to newly instantiated {@link Follower}s. No relation to
     *            the ID used by {@link #startMeasuring(MessageMeasure, String)}
     *            .
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
}
