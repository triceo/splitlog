package com.github.triceo.splitlog.api;

import java.util.concurrent.TimeUnit;

/**
 * Allows users to track various statistics on classes implementing
 * {@link MessageMetricProducer}. These classes are intended for measuring only,
 * and will therefore not store the {@link Message}s that have passed through
 * them.
 *
 * @param <T>
 *            Type of the value that the metric is measuring.
 */
public interface MessageMetric<T extends Number> {

    /**
     * Retrieves the measure that is used to produce the value of this metric.
     *
     * @return Never null.
     */
    MessageMeasure<T> getMeasure();

    /**
     * Retrieve the number of times that the metric has been invoked on a
     * {@link Message}.
     *
     * @return A number >= 0.
     */
    long getMessageCount();

    /**
     * Retrieve the value of this metric, which is a sum of the return values of
     * all of this metric's {@link MessageMeasure} invocations.
     *
     * @return Whatever, depends on the measure. Initial value, before any
     *         messages arrive, is null
     */
    T getValue();

    /**
     * Will block until a message arrives, for which the condition is true. If
     * none arrives before the timeout, it unblocks anyway.
     *
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return The message that made the metric pass the condition. Null if the
     *         method unblocked due to some other reason.
     */
    Message waitFor(MessageMetricCondition<T> condition);

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
     * @return The message that made the metric pass the condition. Null if the
     *         method unblocked due to some other reason.
     */
    Message waitFor(MessageMetricCondition<T> condition, long timeout, TimeUnit unit);

}
