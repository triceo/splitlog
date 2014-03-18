package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.Message;

/**
 * Allows users to filter messages based on their own criteria.
 */
public interface MessageCondition {

    /**
     * Evaluate a message against a user-provided condition.
     * 
     * @param evaluate
     *            The message to evaluate.
     * @return True if message matches.
     */
    boolean accept(Message evaluate);

}
