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
     * later, by either {@link #ACCEPTED} or {@link #REJECTED}.
     */
    INCOMING,
    /**
     * The message has been read fully, but rejected by a user-defined message
     * acceptance filter on a {@link LogWatch}. Terminal state.
     */
    REJECTED;

}
