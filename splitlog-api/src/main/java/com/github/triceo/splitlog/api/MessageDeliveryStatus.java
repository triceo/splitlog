package com.github.triceo.splitlog.api;

/**
 * Various states that a {@link Message} can be in while within the system.
 */
public enum MessageDeliveryStatus {

    /**
     * Message fully read and accepted into the {@link LogWatch}. Terminal
     * state.
     */
    ACCEPTED,
    /**
     * Message not yet fully read. Is purely temporary and will be replaced
     * later, by either {@link #UNDECIDED} or {@link #UNDELIVERED}.
     */
    INCOMING,
    /**
     * The message has been read fully, but rejected by a user-defined message
     * acceptance filter on a {@link LogWatch}. Terminal state.
     */
    REJECTED,
    /**
     * The message has been fully read, but hasn't been through the acceptance
     * filter yet. Will be replaced by either {@link #ACCEPTED} or
     * {@link #REJECTED}.
     */
    UNDECIDED,
    /**
     * {@link Follower} terminated before this message could be read fully.
     * Terminal state.
     */
    UNDELIVERED;

}
