package com.github.triceo.splitlog.api;

/**
 * Allows users to specify if they want such messages to be stored in their
 * {@link LogWatch}.
 *
 * Messages that pass this condition will be sent to {@link Follower}s as
 * {@link MessageDeliveryStatus#ACCEPTED}.
 * {@link MessageDeliveryStatus#REJECTED} is for when the message doesn't pass
 * this condition.
 */
public interface MessageStoreCondition {

    /**
     * Evaluate a message against a user-provided condition.
     *
     * @param evaluate
     *            The message to evaluate.
     * @param status
     *            Current processing status of the message.
     * @param source
     *            The notifying object.
     * @return True if the message should be stored.
     */
    boolean accept(Message evaluate, LogWatch source);
}
