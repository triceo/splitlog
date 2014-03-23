package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.Follower;
import com.github.triceo.splitlog.LogWatch;
import com.github.triceo.splitlog.Message;
import com.github.triceo.splitlog.MessageDeliveryStatus;

/**
 * This will accept any message.
 */
public class AllMessagesAcceptingCondition implements MessageCondition, MessageDeliveryCondition {

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
