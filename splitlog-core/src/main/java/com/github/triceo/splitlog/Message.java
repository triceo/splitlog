package com.github.triceo.splitlog;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.github.triceo.splitlog.exceptions.ExceptionDescriptor;

/**
 * A set of lines from the watched file, that is likely to constitute a single
 * log message.
 */
final public class Message {

    private final String[] lines;
    private final MessageSeverity severity;
    private final MessageType type;
    private final long millisecondsSinceJanuary1st1970;
    private final ExceptionDescriptor exceptionDescriptor;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * {@link #Message(Collection, Date, MessageType)} with the current time and
     * {@link MessageType#LOG}.
     * 
     * @param raw
     *            Message lines.
     */
    protected Message(final Collection<String> raw) {
        this(raw, Calendar.getInstance().getTime(), MessageType.LOG);
    }

    /**
     * {@link #Message(Collection, Date, MessageType, MessageSeverity)} with
     * {@link MessageType#LOG}.
     * 
     * @param raw
     *            Message lines.
     * @param d
     *            Timestamp of the message.
     * @param severity
     *            Message severity.
     */
    protected Message(final Collection<String> raw, final Date d, final MessageSeverity severity) {
        this(raw, d, MessageType.LOG, severity);
    }

    /**
     * {@link #Message(Collection, Date, MessageType, MessageSeverity)} with
     * {@link MessageSeverity#UNKNOWN}.
     * 
     * @param raw
     *            Message lines.
     * @param d
     *            Timestamp of the message.
     * @param type
     *            Message type
     */
    protected Message(final Collection<String> raw, final Date d, final MessageType type) {
        this(raw, d, type, MessageSeverity.UNKNOWN);
    }

/**
     * {@link #Message(Collection, Date, MessageType, MessageSeverity, ExceptionDescriptor) with null exception descriptor.
     * 
     * @param raw
     *            Message lines.
     * @param d
     *            Timestamp of the message.
     * @param type
     *            Message type
     * @param severity
     *            Message severity.
     */
    protected Message(final Collection<String> raw, final Date d, final MessageType type, final MessageSeverity severity) {
        this(raw, d, type, severity, null);
    }

    /**
     * Form a new message with the given properties.
     * 
     * @param raw
     *            Message lines, a non-empty collection.
     * @param d
     *            Timestamp of the message, must not be null.
     * @param type
     *            Message type, must not be null.
     * @param severity
     *            Message severity, must not be null.
     * @param exception
     *            Exception descriptor, or null if message is to contain no
     *            exception data.
     */
    protected Message(final Collection<String> raw, final Date d, final MessageType type,
            final MessageSeverity severity, final ExceptionDescriptor exception) {
        if ((raw == null) || raw.isEmpty()) {
            throw new IllegalArgumentException("Message must not be null.");
        } else if (severity == null) {
            throw new IllegalArgumentException("Severity must not be null.");
        } else if (type == null) {
            throw new IllegalArgumentException("Type must not be null.");
        }
        this.lines = raw.toArray(new String[raw.size()]);
        this.severity = severity;
        this.type = type;
        this.millisecondsSinceJanuary1st1970 = d.getTime();
        this.exceptionDescriptor = exception;
    }

    /**
     * Creates a one-line message of type {@link MessageType#TAG}.
     * 
     * @param message
     *            The only line in the message.
     */
    protected Message(final String message) {
        if ((message == null) || (message.length() == 0)) {
            throw new IllegalArgumentException("Message must not be empty.");
        }
        this.lines = new String[] { message.trim() };
        this.severity = MessageSeverity.UNKNOWN;
        this.type = MessageType.TAG;
        this.millisecondsSinceJanuary1st1970 = System.currentTimeMillis();
        this.exceptionDescriptor = null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Message other = (Message) obj;
        if (!Arrays.equals(this.lines, other.lines)) {
            return false;
        }
        if (this.severity != other.severity) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }

    public Date getDate() {
        return new Date(this.millisecondsSinceJanuary1st1970);
    }

    /**
     * Get data about exception included in this message.
     * 
     * @return Exception data, if {@link #hasException()} returns true. Null in
     *         any other case.
     */
    public ExceptionDescriptor getExceptionDescriptor() {
        return this.exceptionDescriptor;
    }

    /**
     * This method will create a brand new unmodifiable list every time it is
     * called. Use with caution.
     * 
     * @return Unmodifiable representation of lines in this message.
     */
    public List<String> getLines() {
        return Collections.unmodifiableList(Arrays.asList(this.lines));
    }

    public MessageSeverity getSeverity() {
        return this.severity;
    }

    public MessageType getType() {
        return this.type;
    }

    public boolean hasException() {
        return this.exceptionDescriptor != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + Arrays.hashCode(this.lines);
        result = (prime * result) + ((this.severity == null) ? 0 : this.severity.hashCode());
        result = (prime * result) + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getDate());
        sb.append(" (");
        sb.append(this.type);
        sb.append(") ");
        sb.append(this.severity);
        sb.append(" ");
        for (final String line : this.lines) {
            sb.append(line);
            sb.append(Message.LINE_SEPARATOR);
        }
        return sb.toString();
    }

}
