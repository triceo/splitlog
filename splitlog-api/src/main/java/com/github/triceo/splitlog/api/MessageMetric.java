package com.github.triceo.splitlog.api;

/**
 * Allows users to track various statistics on classes implementing
 * {@link MessageMetricProducer}.
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
    long countMessages();

    /**
     * Retrieve the value of this metric, which is a sum of the return values of
     * all of this metric's {@link MessageMeasure} invocations.
     *
     * @return Whatever, depends on the measure.
     */
    T getValue();

}
