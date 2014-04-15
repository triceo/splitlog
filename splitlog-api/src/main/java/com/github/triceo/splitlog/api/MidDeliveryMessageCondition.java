package com.github.triceo.splitlog.api;

/**
 * Allows users to filter messages based on their own criteria. These conditions
 * will be used whenever the {@link LogWatch} and its {@link Follower}s need to
 * notify other parts of code about newly received {@link Message}s.
 *
 * @param <S>
 *            Where this is getting its {@link Message}s from.
 */
public interface MidDeliveryMessageCondition<S extends MessageProducer<S>> {

    /**
     * Evaluate a message against a user-provided condition.
     *
     * @param evaluate
     *            The message to evaluate.
     * @param status
     *            Current processing status of the message.
     * @param source
     *            The notifying object.
     * @return True if message matches.
     */
    boolean accept(Message evaluate, MessageDeliveryStatus status, S source);

}
