package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.api.SimpleMessageCondition;

/**
 * This will accept any message.
 */
public final class AllMessagesAcceptingCondition implements MidDeliveryMessageCondition, SimpleMessageCondition {

    public static final AllMessagesAcceptingCondition INSTANCE = new AllMessagesAcceptingCondition();

    private AllMessagesAcceptingCondition() {
        // singleton
    }

    @Override
    public boolean accept(final Message evaluate) {
        return true;
    }

    @Override
    public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final Follower source) {
        return this.accept(evaluate);
    }

    @Override
    public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
        return this.accept(evaluate);
    }

}
