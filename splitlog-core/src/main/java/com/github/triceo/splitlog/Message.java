package com.github.triceo.splitlog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// FIXME implement message date
public class Message {

    private final List<String> lines;
    private final MessageSeverity severity;
    private final MessageType type;
    private final Date date;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public Message(final Collection<String> raw) {
        this(raw, Calendar.getInstance().getTime(), MessageType.LOG);
    }

    public Message(final Collection<String> raw, final Date d, final MessageSeverity severity) {
        this(raw, d, MessageType.LOG, severity);
    }

    public Message(final Collection<String> raw, final Date d, final MessageType type) {
        this(raw, d, type, MessageSeverity.UNKNOWN);
    }

    public Message(final Collection<String> raw, final Date d, final MessageType type, final MessageSeverity severity) {
        if ((raw == null) || raw.isEmpty()) {
            throw new IllegalArgumentException("Message must not be null.");
        } else if (severity == null) {
            throw new IllegalArgumentException("Severity must not be null.");
        } else if (type == null) {
            throw new IllegalArgumentException("Type must not be null.");
        }
        this.lines = Collections.unmodifiableList(new ArrayList<String>(raw));
        this.severity = severity;
        this.type = type;
        this.date = (Date) d.clone();
    }

    /**
     * Creates a one-line message of type {@link MessageType#TAG}.
     */
    public Message(final String message) {
        if ((message == null) || (message.length() == 0)) {
            throw new IllegalArgumentException("Message must not be empty.");
        }
        final List<String> lines = new ArrayList<String>();
        lines.add(message);
        this.lines = Collections.unmodifiableList(lines);
        this.severity = MessageSeverity.UNKNOWN;
        this.type = MessageType.TAG;
        this.date = Calendar.getInstance().getTime();
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
        if (this.lines == null) {
            if (other.lines != null) {
                return false;
            }
        } else if (!this.lines.equals(other.lines)) {
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
        return this.date;
    }

    public List<String> getLines() {
        return this.lines;
    }

    public MessageSeverity getSeverity() {
        return this.severity;
    }

    public MessageType getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.lines == null) ? 0 : this.lines.hashCode());
        result = (prime * result) + ((this.severity == null) ? 0 : this.severity.hashCode());
        result = (prime * result) + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.date);
        sb.append(" (");
        sb.append(this.type);
        sb.append(") ");
        sb.append(this.severity);
        for (final String line : this.lines) {
            sb.append(line);
            sb.append(Message.LINE_SEPARATOR);
        }
        return sb.toString();
    }

}
