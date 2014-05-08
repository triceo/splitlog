package com.github.triceo.splitlog.exchanges;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageProducer;

final class MeasuringMessageExchange<T extends Number, S extends MessageProducer<S>> extends
        AbstractMessageExchange<MessageMetricCondition<T, S>, S> {

    private final MessageMetric<T, S> metric;

    public MeasuringMessageExchange(final MessageMetric<T, S> metric, final MessageMetricCondition<T, S> condition) {
        super(condition);
        this.metric = metric;
    }

    @Override
    protected boolean isAccepted(final Message msg, final MessageDeliveryStatus status, final S source) {
        return this.getBlockingCondition().accept(this.metric);
    }

}
