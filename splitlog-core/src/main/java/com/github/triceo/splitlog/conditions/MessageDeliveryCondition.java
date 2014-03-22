package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.Follower;
import com.github.triceo.splitlog.LogWatch;
import com.github.triceo.splitlog.MergingFollower;
import com.github.triceo.splitlog.Message;
import com.github.triceo.splitlog.MessageDeliveryStatus;

/**
 * Allows users to filter messages based on their own criteria.
 */
public interface MessageDeliveryCondition {

    /**
     * Evaluate a message against a user-provided condition when the message has
     * been received from a {@link LogWatch}.
     * 
     * @param evaluate
     *            The message to evaluate.
     * @param status
     *            Current processing status of the message.
     * @param source
     *            The notifying object.
     * @return True if message matches.
     */
    boolean accept(Message evaluate, MessageDeliveryStatus status, LogWatch source);

    /**
     * Evaluate a message against a user-provided condition when the message has
     * been received from a {@link Follower}. This will happen when the message
     * condition evaluation happens within {@link MergingFollower}.
     * 
     * @param evaluate
     *            The message to evaluate.
     * @param status
     *            Current processing status of the message.
     * @param source
     *            The notifying object.
     * @return True if message matches.
     */
    boolean accept(Message evaluate, MessageDeliveryStatus status, Follower source);
}
