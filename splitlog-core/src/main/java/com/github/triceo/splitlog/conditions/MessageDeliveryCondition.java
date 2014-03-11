package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.Message;
import com.github.triceo.splitlog.MessageDeliveryStatus;

/**
 * Allows users to filter messages based on their own criteria.
 */
public interface MessageDeliveryCondition {

    /**
     * Evaluate a message against a user-provided condition.
     * 
     * @param evaluate
     *            The message to evaluate.
     * @param status
     *            Current processing status of the message.
     * @return True if message matches.
     */
    boolean accept(Message evaluate, MessageDeliveryStatus status);

}
