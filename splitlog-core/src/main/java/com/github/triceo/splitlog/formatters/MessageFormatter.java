package com.github.triceo.splitlog.formatters;

import com.github.triceo.splitlog.Message;

/**
 * Converts a message to a textual representation.
 * 
 */
public interface MessageFormatter {

    /**
     * Platform-specific line separator.
     */
    static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * Provides a textual representation of a message.
     * 
     * @param m
     *            Message in question.
     * @return Message as a string.
     */
    String format(Message m);

}
