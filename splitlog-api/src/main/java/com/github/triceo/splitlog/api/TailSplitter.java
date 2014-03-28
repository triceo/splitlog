package com.github.triceo.splitlog.api;

import java.util.Date;
import java.util.List;

/**
 * The purpose of classes implementing this interface is to interpret the log
 * files. Each implementation should be familiar with one type of log file (i.e.
 * JBoss logging or Logback) and be able to interpret the messages and their
 * lines.
 */
public interface TailSplitter {

    /**
     * Read the message and find the date when the message was submitted.
     * 
     * @param raw
     *            Raw, untreated lines of the message.
     * @return Date from the message, or null if not found.
     */
    Date determineDate(final List<String> raw);

    /**
     * Read the message and try to identify an exception stack trace within.
     * 
     * @param raw
     *            Raw, untreated lines of the message.
     * @return Exception data if found, null otherwise.
     */
    ExceptionDescriptor determineException(final List<String> raw);

    /**
     * Read the message and try to find its severity.
     * 
     * @param raw
     *            Raw, untreated lines of the message.
     * @return Severity included in the message, {@link MessageSeverity#UNKNOWN}
     *         otherwise.
     */
    MessageSeverity determineSeverity(final List<String> raw);

    /**
     * Read the message and try to find its type.
     * 
     * @param raw
     *            Raw, untreated lines of the message.
     * @return Type guessed from the message, {@link MessageType#LOG} if
     *         undetermined.
     */
    MessageType determineType(final List<String> raw);

    /**
     * Whether or not this particular line from the log starts a new log
     * message. If so, it will be used to trigger a {@link Message} being
     * produced and possibly stored.
     * 
     * @param line
     *            Line from the log.
     * @return True if this is the first line of a new log message.
     */
    boolean isStartingLine(final String line);

    /**
     * Take a line from the log and attempt to strip it of metadata, such as
     * severity, type and date.
     * 
     * @param line
     *            Line from the log.
     * @return The same line, stripped of metadata. Usually just the actual
     *         information being logged.
     */
    String stripOfMetadata(final String line);

}
