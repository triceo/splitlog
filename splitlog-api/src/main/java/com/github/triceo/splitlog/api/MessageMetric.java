package com.github.triceo.splitlog.api;

/**
 * Allows users to track various statistics on classes implementing
 * {@link MessageProducer}. These classes are intended for measuring only, and
 * will therefore not store the {@link Message}s that have passed through them.
 *
 * @param <T>
 *            Type of the value that the metric is measuring.
 * @param <S>
 *            Where this is getting its {@link Message}s from.
 */
public interface MessageMetric<T extends Number, S extends MessageProducer<S>> extends MessageConsumer<S>,
SupportsExpectations<S, MessageMetricCondition<T, S>> {

    /**
     * Retrieves the measure that is used to produce the value of this metric.
     *
     * @return Never null.
     */
    MessageMeasure<T, S> getMeasure();

    /**
     * Retrieve the number of times that the metric has been invoked on a
     * {@link Message}.
     *
     * @return A number >= 0.
     */
    long getMessageCount();

    /**
     * Retrieve the number of times that the metric has been invoked on a
     * {@link Message}, at the time immediately after a given message has been
     * processed.
     *
     * @param timestamp
     *            The point in time after this message was processed.
     * @return 0 when timestamp is null. -1 in case no such message was ever
     *         processed by this metric. The proper value otherwise.
     */
    long getMessageCount(Message timestamp);

    /**
     * Retrieve the instance that is responsible for notifying this metric of
     * new {@link Message}s-
     *
     * @return Typically the instance that was used to retrieve this metric.
     */
    S getSource();

    /**
     * Retrieve the value of this metric, which is a sum of the return values of
     * all of this metric's {@link MessageMeasure} invocations.
     *
     * @return Whatever, depends on the measure. Initial value, before any
     *         messages arrive, is null
     */
    T getValue();

    /**
     * Retrieve the value of this metric, which is a sum of the return values of
     * all of this metric's {@link MessageMeasure} invocations, at the time
     * immediately after a given message has been processed.
     *
     * @param timestamp
     *            The point in time after this message was processed. Null will
     *            retrieve the initial state.
     * @return Whatever, depends on the measure. Initial state, before any
     *         messages arrive, is {@link MessageMeasure#initialValue()}. Null
     *         is returned in case no such message was ever processed by this
     *         metric.
     */
    T getValue(Message timestamp);

}
