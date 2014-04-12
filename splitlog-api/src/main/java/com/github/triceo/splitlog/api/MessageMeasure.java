package com.github.triceo.splitlog.api;

/**
 * Used by {@link MessageMetric} to determine how much a {@link Message} is worth.
 *
 * @param <T>
 *            The value type returned by the metric.
 */
public interface MessageMeasure<T extends Number> {

    /**
     * Assign a numeric value to a given message.
     *
     * @param evaluate
     *            The message to evaluate.
     * @param status
     *            Current processing status of the message.
     * @param source
     *            The notifying object.
     * @return The value to be added to the metric.
     */
    T evaluate(Message evaluate, MessageDeliveryStatus status, Follower source);

    /**
     * Assign a numeric value to a given message.
     *
     * @param evaluate
     *            The message to evaluate.
     * @param status
     *            Current processing status of the message.
     * @param source
     *            The notifying object.
     * @return The value to be added to the metric.
     */
    T evaluate(Message evaluate, MessageDeliveryStatus status, LogWatch source);

}
