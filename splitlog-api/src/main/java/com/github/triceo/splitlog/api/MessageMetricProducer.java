package com.github.triceo.splitlog.api;

/**
 * Implementors of this interface provide users with means of measuring various
 * properties of {@link Message}s that pass through them.
 */
public interface MessageMetricProducer {

    /**
     * Request that a message property be tracked from now on.
     *
     * @param measure
     *            The class that measures the given property.
     * @param id
     *            Unique identifier by which to locate the metric later.
     * @return The metric to query for the value of the given property.
     * @throws IllegalArgumentException
     *             When a given ID has already been passed to
     *             {@link #measure(MessageMeasure, String)} and not to
     *             {@link #terminateMeasuring(String)} or equivalents.
     */
    <T extends Number> MessageMetric<T> measure(MessageMeasure<T> measure, String id);

    /**
     * Retrieve the metric for a particular ID.
     * 
     * @param id
     *            The ID under which the metric has been requested in
     *            {@link #measure(MessageMeasure, String)}.
     * @return Null if no such metric. Either not
     *         {@link #measure(MessageMeasure, String)}'d or already
     *         {@link #terminateMeasuring(MessageMeasure)}'d.
     */
    MessageMetric<? extends Number> getMetric(String id);

    /**
     * Retrieve the ID for a particular measure.
     * 
     * @param measure
     *            The metric retrieved by
     *            {@link #measure(MessageMeasure, String)}.
     * @return Null if no such metric. Either not
     *         {@link #measure(MessageMeasure, String)}'d or already
     *         {@link #terminateMeasuring(MessageMeasure)}'d.
     */
    String getMetricId(MessageMetric<? extends Number> measure);

    /**
     * Will stop the metric from being notified of new {@link Message}s. From
     * this point on, the ID will become available for taking.
     *
     * @param id
     *            ID of the metric in question.
     * @return True if stopped, false if unknown.
     */
    boolean terminateMeasuring(String id);

    /**
     * Will stop the metric from being notified of new {@link Message}s. From
     * this point on, the ID will become available for taking.
     *
     * @param measure
     *            The metric in question.
     * @return True if stopped, false if unknown.
     */
    boolean terminateMeasuring(MessageMeasure<? extends Number> measure);

}
