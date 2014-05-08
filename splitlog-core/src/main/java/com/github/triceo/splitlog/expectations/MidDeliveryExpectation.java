package com.github.triceo.splitlog.expectations;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;

final class MidDeliveryExpectation<S extends MessageProducer<S>> extends
        AbstractExpectation<MidDeliveryMessageCondition<S>, S> {

    public MidDeliveryExpectation(final MidDeliveryExpectationManager<S> manager,
        final MidDeliveryMessageCondition<S> condition) {
        super(manager, condition);
    }

    @Override
    protected boolean isAccepted(final Message msg, final MessageDeliveryStatus status, final S source) {
        return this.getBlockingCondition().accept(msg, status, source);
    }

}
