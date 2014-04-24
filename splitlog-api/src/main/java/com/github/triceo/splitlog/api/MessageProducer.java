package com.github.triceo.splitlog.api;

/**
 * Implementors of this interface provide users with means of measuring various
 * properties of {@link Message}s that pass through them.
 *
 * @param <P>
 *            Where this is getting its {@link Message}s from.
 */
public interface MessageProducer<P extends MessageProducer<P>> {

    /**
     * Return the number of consumers that have been
     * {@link #startConsuming(MessageListener)}'d and not
     * {@link #stopConsuming(MessageConsumer)}'d.
     *
     * @return Always >= 0.
     */
    int countConsumers();

    /**
     * Return the number of metrics that have been
     * {@link #startMeasuring(MessageMeasure, String)}'d and not
     * {@link #stopMeasuring(String)}'d.
     *
     * @return Always >= 0.
     */
    int countMetrics();

    /**
     * Retrieve the metric for a particular ID.
     *
     * @param id
     *            The ID under which the metric has been requested in
     *            {@link #startMeasuring(MessageMeasure, String)}.
     * @return Null if no such metric. Either not
     *         {@link #startMeasuring(MessageMeasure, String)}'d or already
     *         {@link #stopMeasuring(MessageMetric)}'d.
     */
    MessageMetric<? extends Number, P> getMetric(String id);

    /**
     * Retrieve the ID for a particular measure.
     *
     * @param measure
     *            The metric retrieved by
     *            {@link #startMeasuring(MessageMeasure, String)}.
     * @return Null if no such metric. Either not
     *         {@link #startMeasuring(MessageMeasure, String)}'d or already
     *         {@link #stopMeasuring(MessageMetric)}'d.
     */
    String getMetricId(MessageMetric<? extends Number, P> measure);

    /**
     * Whether or not the particular message consumer is being notified of new
     * messages.
     *
     * @param consumer
     *            Consumer in question.
     * @return True if called between {@link #startConsuming(MessageListener)}
     *         and {@link #stopConsuming(MessageConsumer)}.
     */
    boolean isConsuming(MessageConsumer<P> consumer);

    /**
     * Whether or not particular {@link MessageMetric} is active.
     *
     * @param metric
     *            Metric in question.
     * @return True after {@link #startMeasuring(MessageMeasure, String)} has
     *         been called and before {@link #stopMeasuring(MessageMetric)}.
     */
    boolean isMeasuring(MessageMetric<? extends Number, P> metric);

    /**
     * Whether or not particular {@link MessageMetric} is active.
     *
     * @param id
     *            ID of the metric in question.
     * @return True after {@link #startMeasuring(MessageMeasure, String)} has
     *         been called and before {@link #stopMeasuring(String)}.
     */
    boolean isMeasuring(String id);

    /**
     * Register a listener to be notified of new messages in this producer.
     *
     * @param listener
     *            Listener in question.
     * @return A newly produced consumer.
     * @throws IllegalArgumentException
     *             When <code>listener instanceof {@link MessageConsumer}</code>
     *             .
     */
    MessageConsumer<P> startConsuming(MessageListener<P> listener);

    /**
     * Request that a message property be tracked from now on.
     *
     * @param measure
     *            The class that measures the given property. It is highly
     *            recommended that a measure instance be exclusive to each
     *            metric.
     * @param id
     *            Unique identifier by which to locate the metric later.
     * @return The metric to query for the value of the given property.
     * @throws IllegalArgumentException
     *             When a given ID has already been passed to
     *             {@link #startMeasuring(MessageMeasure, String)} and not to
     *             {@link #stopMeasuring(String)} or equivalents.
     */
    <T extends Number> MessageMetric<T, P> startMeasuring(MessageMeasure<T, P> measure, String id);

    /**
     * Tell a consumer to no longer listen to new messages in this producer.
     *
     * @param consumer
     *            Consumer in question.
     * @return True, unless no longer {@link #isConsuming(MessageConsumer)}.
     */
    boolean stopConsuming(MessageConsumer<P> consumer);

    /**
     * Will stop the metric from being notified of new {@link Message}s. From
     * this point on, the ID will become available for taking.
     *
     * @param metric
     *            The metric in question.
     * @return True if stopped, false if unknown.
     */
    boolean stopMeasuring(MessageMetric<? extends Number, P> metric);

    /**
     * Will stop the metric from being notified of new {@link Message}s. From
     * this point on, the ID will become available for taking.
     *
     * @param id
     *            ID of the metric in question.
     * @return True if stopped, false if unknown.
     */
    boolean stopMeasuring(String id);

}
