package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.api.TailSplitter;
import com.github.triceo.splitlog.splitters.JBossServerLogTailSplitter;
import com.github.triceo.splitlog.splitters.SimpleTailSplitter;

/**
 * This will accept any {@link Message} whose {@link Message#getLogger()}
 * doesn't start with com.github.triceo.splitlog.
 *
 * This requires that the {@link JBossServerLogTailSplitter} or any other smart
 * {@link TailSplitter} be used. {@link SimpleTailSplitter} will not provide the
 * logger information and therefore this condition will not work together with.
 */
public final class SplitlogMessagesRejectingCondition implements SimpleMessageCondition {

    public static final SimpleMessageCondition INSTANCE = new SplitlogMessagesRejectingCondition();

    private SplitlogMessagesRejectingCondition() {
        // singleton
    }

    @Override
    public boolean accept(final Message evaluate) {
        return !evaluate.getLogger().startsWith("com.github.triceo.splitlog");
    }

}
