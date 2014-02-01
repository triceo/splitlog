package com.github.triceo.splitlog;

import java.util.ArrayList;
import java.util.List;

public class Message {

    private final RawMessage rawMessage;
    private final MessageSeverity severity;
    private final MessageType type;

    public Message(final RawMessage raw) {
        this(raw, null);
    }

    public Message(final RawMessage raw, final MessageClassifier<MessageType> typeClassifier) {
        this(raw, typeClassifier, null);
    }

    public Message(final RawMessage raw, final MessageClassifier<MessageType> typeClassifier,
            final MessageClassifier<MessageSeverity> severityClassifier) {
        if (raw == null) {
            throw new IllegalArgumentException("Message must not be null.");
        }
        this.rawMessage = raw;
        this.severity = (severityClassifier == null) ? MessageSeverity.UNKNOWN : severityClassifier.classify(raw);
        this.type = (typeClassifier == null) ? MessageType.TAG : typeClassifier.classify(raw);
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
        this.rawMessage = new RawMessage(lines);
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
        if (this.rawMessage == null) {
            if (other.rawMessage != null) {
                return false;
            }
        } else if (!this.rawMessage.equals(other.rawMessage)) {
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

    public RawMessage getRawMessage() {
        return this.rawMessage;
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
        result = (prime * result) + ((this.rawMessage == null) ? 0 : this.rawMessage.hashCode());
        result = (prime * result) + ((this.severity == null) ? 0 : this.severity.hashCode());
        result = (prime * result) + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

}
