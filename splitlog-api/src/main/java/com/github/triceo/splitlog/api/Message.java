package com.github.triceo.splitlog.api;

import java.util.Date;
import java.util.List;

/**
 * A set of lines from the watched file, that is likely to constitute a single
 * log message. No two messages are allowed to be equal, unless they have the
 * same {@link #getUniqueId()}.
 */
public interface Message {

    /**
     * Get the date that this message was logged on.
     * 
     * @return Newly constructed instance of {@link Date} with the message log
     *         timestamp.
     */
    Date getDate();

    /**
     * Get data about exception included in this message.
     * 
     * @return Exception data, if {@link #hasException()} returns true. Null in
     *         any other case.
     */
    ExceptionDescriptor getExceptionDescriptor();

    /**
     * Get each line of the message.
     * 
     * @return Unmodifiable representation of lines in this message, exactly as
     *         were received.
     */
    List<String> getLines();

    /**
     * Get each line of the message, with metadata stripped out.
     * 
     * @return Unmodifiable representation of text of the message.
     */
    List<String> getLinesWithoutMetadata();

    /**
     * Return a message that preceded this one in the same log stream.
     * 
     * @return Null if there was no such message, the message already got GC'd,
     *         or <code>{@link #getType()} == {@link MessageType#TAG}</code>.
     */
    Message getPreviousMessage();

    MessageSeverity getSeverity();

    MessageType getType();

    /**
     * Unique ID of the message, that can be used to compare messages in the
     * order of their arrival into this tool.
     * 
     * @return ID of the message, guaranteed to be unique for every message, and
     *         increasing from message to message.
     */
    long getUniqueId();

    boolean hasException();

}
