package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.api.SimpleMessageCondition;

/**
 * This will accept any message.
 */
public final class AllLogWatchMessagesAcceptingCondition implements MidDeliveryMessageCondition<Follower>,
        SimpleMessageCondition {

    public static final AllLogWatchMessagesAcceptingCondition INSTANCE = new AllLogWatchMessagesAcceptingCondition();

    private AllLogWatchMessagesAcceptingCondition() {
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

}
