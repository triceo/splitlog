package com.github.triceo.splitlog.api;

/**
 * Allows users to filter messages based on their own criteria.
 */
public interface MessageCondition {

    /**
     * Evaluate a message against a user-provided condition, taking into account
     * where that message comes from.
     * 
     * Will be executed by {@link MergingFollower}s as they're notified of
     * {@link Message}s by {@link Follower}s.
     * 
     * @param evaluate
     *            The message to evaluate.
     * @param source
     *            Where does this message come from.
     * @return True if message matches.
     */
    boolean accept(Message evaluate, Follower source);

    /**
     * Evaluate a message against a user-provided condition, taking into account
     * where that message comes from.
     * 
     * Will be executed by {@link Follower}s as they're notified of
     * {@link Message}s by {@link LogWatch}.
     * 
     * @param evaluate
     *            The message to evaluate.
     * @param source
     *            Where does this message come from.
     * @return True if message matches.
     */
    boolean accept(Message evaluate, LogWatch source);
}
