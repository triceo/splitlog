package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageCondition;
import com.github.triceo.splitlog.api.MessageDeliveryCondition;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;

/**
 * This will accept any message.
 */
public final class AllMessagesAcceptingCondition implements MessageCondition, MessageDeliveryCondition {

    public static final AllMessagesAcceptingCondition INSTANCE = new AllMessagesAcceptingCondition();

    private AllMessagesAcceptingCondition() {
        // singleton
    }

    @Override
    public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
        return true;
    }

    @Override
    public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final Follower source) {
        return true;
    }

    @Override
    public boolean accept(final Message evaluate, final Follower source) {
        return true;
    }

    @Override
    public boolean accept(final Message evaluate, final LogWatch source) {
        return true;
    }
}
