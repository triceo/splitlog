package com.github.triceo.splitlog.api;

/**
 * Used by {@link MessageMetric} to determine how much a {@link Message} is
 * worth. Users are discouraged from implementing these as stateful - doing so
 * will result in unpredictable behavior, as a single instance of the class can
 * be shared across multiple {@link MessageMetric}s.
 *
 * @param <T>
 *            The value type returned by the metric.
 * @param <S>
 *            Where this is getting its {@link Message}s from.
 */
public interface MessageMeasure<T extends Number, S extends MessageSource<S>> {

    /**
     * Update metric after the arrival of another message.
     *
     * @param metric
     *            The metric that is being updated.
     * @param evaluate
     *            The message to evaluate.
     * @param status
     *            Current processing status of the message.
     * @param source
     *            The notifying object.
     * @return The new value for the metric. Message count will be incremented
     *         automatically.
     */
    T update(MessageMetric<T, S> metric, Message evaluate, MessageDeliveryStatus status, S source);

}
