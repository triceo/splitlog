package com.github.triceo.splitlog.expectations;

import com.github.triceo.splitlog.api.MessageAction;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;

public final class MidDeliveryExpectationManager<P extends MessageProducer<P>> extends
AbstractExpectationManager<P, MidDeliveryMessageCondition<P>> {

    @Override
    protected AbstractExpectation<MidDeliveryMessageCondition<P>, P> createExpectation(
            final MidDeliveryMessageCondition<P> condition, final MessageAction<P> action) {
        return new MidDeliveryExpectation<P>(this, condition, action);
    }

}
