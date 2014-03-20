package com.github.triceo.splitlog;

/**
 * Various states that a {@link Message} can be in while within the system. Only
 * messages with status {@link MessageDeliveryStatus#ACCEPTED} will be stored in
 * {@link MessageStore} and will be available for output
 */
public enum MessageDeliveryStatus {

    /**
     * Message not yet fully read. Is purely temporary and will be replaced
     * later.
     */
    INCOMING,
    /**
     * Message fully read and accepted into the {@link LogWatch}.
     */
    ACCEPTED,
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
