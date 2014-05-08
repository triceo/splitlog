package com.github.triceo.splitlog.exchanges;

import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;

public final class MidDeliveryMessageExchangeManager<P extends MessageProducer<P>> extends
        AbstractMessageExchangeManager<P, MidDeliveryMessageCondition<P>> {

    @Override
    protected AbstractMessageExchange<MidDeliveryMessageCondition<P>, P> createExpectation(
        final MidDeliveryMessageCondition<P> condition) {
        return new MidDeliveryMessageExchange<P>(this, condition);
    }

}
