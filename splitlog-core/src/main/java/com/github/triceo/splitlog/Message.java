package com.github.triceo.splitlog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// FIXME implement message date
public class Message {

    private final List<String> lines;
    private final MessageSeverity severity;
    private final MessageType type;

    public Message(final Collection<String> raw) {
        this(raw, MessageType.TAG);
    }

    public Message(final Collection<String> raw, final MessageSeverity severity) {
        this(raw, MessageType.LOG, severity);
    }

    public Message(final Collection<String> raw, final MessageType type) {
        this(raw, type, MessageSeverity.UNKNOWN);
    }

    public Message(final Collection<String> raw, final MessageType type, final MessageSeverity severity) {
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

    // FIXME needs to print line by line
    @Override
    public String toString() {
        return "(" + this.type + ") " + this.severity + " " + this.lines;
    }

}
