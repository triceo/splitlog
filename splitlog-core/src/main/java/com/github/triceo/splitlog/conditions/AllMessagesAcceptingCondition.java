package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.Message;

public class AllMessagesAcceptingCondition implements MessageCondition {

    public static final AllMessagesAcceptingCondition INSTANCE = new AllMessagesAcceptingCondition();

    private AllMessagesAcceptingCondition() {

    }

    @Override
    public boolean accept(final Message evaluate) {
        return true;
    }

}
