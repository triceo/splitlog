package com.github.triceo.splitlog.expectations;

import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageProducer;

public final class MetricExpectationManager<T extends Number, P extends MessageProducer<P>> extends
AbstractExpectationManager<P, MessageMetricCondition<T, P>> {

    private final MessageMetric<T, P> metric;

    public MetricExpectationManager(final MessageMetric<T, P> metric) {
        this.metric = metric;
    }

    @Override
    protected AbstractExpectation<MessageMetricCondition<T, P>, P> createExpectation(
        final MessageMetricCondition<T, P> condition) {
        return new MetricExpectation<T, P>(this, this.metric, condition);
    }

}
