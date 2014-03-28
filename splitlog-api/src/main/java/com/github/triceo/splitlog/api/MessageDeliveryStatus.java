package com.github.triceo.splitlog.api;

/**
 * Various states that a {@link Message} can be in while within the system.
 */
public enum MessageDeliveryStatus {

    /**
     * Message fully read and accepted into the {@link LogWatch}.
     */
    ACCEPTED,
    /**
     * Message not yet fully read. Is purely temporary and will be replaced
     * later.
     */
    INCOMING,
    /**
     * The message has been read fully, but rejected by a user-defined message
     * acceptance filter on a {@link LogWatch}.
     */
    REJECTED,
    /**
     * {@link Follower} terminated before this message could be read fully.
     */
    UNDELIVERED;

}
