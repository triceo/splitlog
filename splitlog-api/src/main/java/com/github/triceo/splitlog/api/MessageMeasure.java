package com.github.triceo.splitlog.api;

/**
 * Used by {@link MessageMetric} to determine how much a {@link Message} is
 * worth.
 *
 * @param <T>
 *            The value type returned by the metric.
 */
public interface MessageMeasure<T extends Number> {

    /**
     * Update metric after the arrival of another message.
     *
     * @param previous
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
    T update(MessageMetric<T> metric, Message evaluate, MessageDeliveryStatus status, MessageSource source);

}
