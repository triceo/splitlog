package com.github.triceo.splitlog.api;

/**
 * Allows users to filter based on their own metric criteria.
 */
public interface MessageMetricCondition<T extends Number> {

    /**
     * Evaluate metric against a user-provided condition.
     *
     * @param evaluate
     *            The metric to evaluate.
     * @return True if the metric matches.
     */
    boolean accept(MessageMetric<T> evaluate);

}
