package com.github.triceo.splitlog.api;

/**
 * Allows users to either accept or reject a message in various situations.
 */
public interface SimpleMessageCondition {

    /**
     * Evaluate a message against a user-provided condition.
     *
     * @param evaluate
     *            The message to evaluate.
     * @return True if the message passes.
     */
    boolean accept(Message evaluate);
}
