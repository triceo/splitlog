package com.github.triceo.splitlog;

/**
 * Type of message from the log.
 */
public enum MessageType {

    /**
     * Regular message from the log.
     */
    LOG,
    /**
     * A line added by {@link LogTailer#tag(String)}.
     */
    TAG,
    /**
     * Line from standard output, that has been captured in the log.
     */
    STDOUT,
    /**
     * Line from the standard error output, that has been captured in the log.
     */
    STDERR;

}
