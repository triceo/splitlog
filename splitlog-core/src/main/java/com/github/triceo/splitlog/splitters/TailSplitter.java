package com.github.triceo.splitlog.splitters;

import java.util.Date;

import com.github.triceo.splitlog.Message;
import com.github.triceo.splitlog.MessageBuilder;
import com.github.triceo.splitlog.MessageSeverity;
import com.github.triceo.splitlog.MessageType;
import com.github.triceo.splitlog.exceptions.ExceptionDescriptor;

/**
 * The purpose of classes implementing this interface is to interpret the log
 * files. Each implementation should be familiar with one type of log file (i.e.
 * {@link JBossServerLogTailSplitter} for JBoss server logs) and be able to
 * interpret the messages and their lines.
 * 
 */
public interface TailSplitter {

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
     * Read the message and find the date when the message was submitted.
     * 
     * @param message
     *            Message in question.
     * @return Date from the message, or the current date if not found.
     */
    Date determineDate(final MessageBuilder message);

    /**
     * Read the message and try to identify an exception stack trace within.
     * 
     * @param message
     *            Message in question.
     * @return Exception data if found, null otherwise.
     */
    ExceptionDescriptor determineException(final MessageBuilder message);

    /**
     * Read the message and try to find its severity.
     * 
     * @param message
     *            Message in question.
     * @return Severity included in the message, {@link MessageSeverity#UNKNOWN}
     *         otherwise.
     */
    MessageSeverity determineSeverity(final MessageBuilder message);

    /**
     * Read the message and try to find its type.
     * 
     * @param message
     *            Message in question.
     * @return Type guessed from the message, {@link MessageType#LOG} if
     *         undetermined.
     */
    MessageType determineType(final MessageBuilder message);

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
