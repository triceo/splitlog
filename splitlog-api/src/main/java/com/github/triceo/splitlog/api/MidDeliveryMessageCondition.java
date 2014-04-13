package com.github.triceo.splitlog.api;

/**
 * Allows users to filter messages based on their own criteria. These conditions
 * will be used whenever the {@link LogWatch} and its {@link Follower}s need to
 * notify other parts of code about newly received {@link Message}s.
 */
public interface MidDeliveryMessageCondition {

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
}
