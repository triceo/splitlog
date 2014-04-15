package com.github.triceo.splitlog.api;

/**
 * Allows users to filter based on their own metric criteria.
 *
 * @param <T>
 *            Numeric type that this metric will count in.
 * @param <S>
 *            Where this is getting its {@link Message}s from.
 */
public interface MessageMetricCondition<T extends Number, S extends MessageProducer<S>> {

    /**
     * Evaluate metric against a user-provided condition.
     *
     * @param evaluate
     *            The metric to evaluate.
     * @return True if the metric matches.
     */
    boolean accept(MessageMetric<T, S> evaluate);

}
