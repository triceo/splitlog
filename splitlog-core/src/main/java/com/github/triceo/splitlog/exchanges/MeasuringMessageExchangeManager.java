package com.github.triceo.splitlog.exchanges;

import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageProducer;

public final class MeasuringMessageExchangeManager<T extends Number, P extends MessageProducer<P>> extends
AbstractMessageExchangeManager<P, MessageMetricCondition<T, P>> {

    private final MessageMetric<T, P> metric;

    public MeasuringMessageExchangeManager(final MessageMetric<T, P> metric) {
        this.metric = metric;
    }

    @Override
    protected AbstractMessageExchange<MessageMetricCondition<T, P>, P> createExpectation(
            final MessageMetricCondition<T, P> condition) {
        return new MeasuringMessageExchange<T, P>(this.metric, condition);
    }

}
