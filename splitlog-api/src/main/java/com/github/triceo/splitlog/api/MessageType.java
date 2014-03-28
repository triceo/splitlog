package com.github.triceo.splitlog.api;

/**
 * Type of message from the log.
 */
public enum MessageType {

    /**
     * Regular message from the log.
     */
    LOG,
    /**
     * Line from the standard error output, that has been captured in the log.
     */
    STDERR,
    /**
     * Line from standard output, that has been captured in the log.
     */
    STDOUT,
    /**
     * A line added by {@link Follower#tag(String)}.
     */
    TAG;

}
