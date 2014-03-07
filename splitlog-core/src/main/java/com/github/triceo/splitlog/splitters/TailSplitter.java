package com.github.triceo.splitlog.splitters;

import java.util.Date;

import com.github.triceo.splitlog.MessageBuilder;
import com.github.triceo.splitlog.MessageSeverity;
import com.github.triceo.splitlog.MessageType;
import com.github.triceo.splitlog.exceptions.ExceptionDescriptor;

public interface TailSplitter {

    /**
     * Whether or not this particular line from the log starts a new log
     * message. If so, it will be used to trigger a Message being produced and
     * possibly stored.
     * 
     * @param line
     *            Line from the log.
     * @return True if this is the first line of a new log message.
     */
    public boolean isStartingLine(final String line);

    /**
     * Read the message and find the date when the message was submitted.
     * 
     * @param message
     *            Message in question.
     * @return Date from the message, or the current date if not found.
     */
    public Date determineDate(final MessageBuilder message);

    /**
     * Read the message and try to identify an exception stack trace within.
     * 
     * @param message
     *            Message in question.
     * @return Exception data if found, null otherwise.
     */
    public ExceptionDescriptor determineException(final MessageBuilder message);

    /**
     * Read the message and try to find its severity.
     * 
     * @param message
     *            Message in question.
     * @return Severity included in the message, {@link MessageSeverity#UNKNOWN}
     *         otherwise.
     */
    public MessageSeverity determineSeverity(final MessageBuilder message);

    /**
     * Read the message and try to find its type.
     * 
     * @param message
     *            Message in question.
     * @return Type guessed from the message, {@value MessageType#LOG} if
     *         undetermined.
     */
    public MessageType determineType(final MessageBuilder message);

    /**
     * Take a line from the log and attempt to strip it of metadata, such as
     * severity, type and date.
     * 
     * @param line
     *            Line from the log.
     * @return The same line, stripped of metadata. Usually just the actual
     *         information being logged.
     */
    public String stripOfMetadata(final String line);

}
