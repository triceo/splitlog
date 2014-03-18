package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.Message;
import com.github.triceo.splitlog.MessageDeliveryStatus;

/**
 * This will accept any message.
 */
public class AllMessagesAcceptingCondition implements MessageCondition, MessageDeliveryCondition {

    public static final AllMessagesAcceptingCondition INSTANCE = new AllMessagesAcceptingCondition();

    private AllMessagesAcceptingCondition() {

    }

    @Override
    public boolean accept(final Message evaluate) {
        return true;
    }

    @Override
    public boolean accept(final Message evaluate, final MessageDeliveryStatus status) {
        return true;
    }

}
