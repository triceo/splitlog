package com.github.triceo.splitlog.expectations;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageProducer;

final class MetricExpectation<T extends Number, S extends MessageProducer<S>> extends
AbstractExpectation<MessageMetricCondition<T, S>, S> {

    private final MessageMetric<T, S> metric;

    public MetricExpectation(final MetricExpectationManager<T, S> manager, final MessageMetric<T, S> metric,
            final MessageMetricCondition<T, S> condition) {
        super(manager, condition);
        this.metric = metric;
    }

    @Override
    protected boolean isAccepted(final Message msg, final MessageDeliveryStatus status, final S source) {
        return this.getBlockingCondition().accept(this.metric);
    }

}
