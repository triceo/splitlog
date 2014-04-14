package com.github.triceo.splitlog.api;

/**
 * Implementors of this interface provide users with means of measuring various
 * properties of {@link Message}s that pass through them.
 *
 * @param <S>
 *            Where this is getting its {@link Message}s from.
 */
public interface MessageMetricProducer<S extends MessageSource<S>> {

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
    MessageMetric<? extends Number, S> getMetric(String id);

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
    String getMetricId(MessageMetric<? extends Number, S> measure);

    /**
     * Whether or not particular {@link MessageMetric} is active.
     *
     * @param metric
     *            Metric in question.
     * @return True after {@link #startMeasuring(MessageMeasure, String)} has
     *         been called and before {@link #stopMeasuring(MessageMetric)}.
     */
    boolean isMeasuring(MessageMetric<? extends Number, S> metric);

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
    <T extends Number> MessageMetric<T, S> startMeasuring(MessageMeasure<T, S> measure, String id);

    /**
     * Will stop the metric from being notified of new {@link Message}s. From
     * this point on, the ID will become available for taking.
     *
     * @param metric
     *            The metric in question.
     * @return True if stopped, false if unknown.
     */
    boolean stopMeasuring(MessageMetric<? extends Number, S> metric);

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
