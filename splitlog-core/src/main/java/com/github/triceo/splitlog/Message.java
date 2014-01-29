package com.github.triceo.splitlog;

public class Message {

    private final RawMessage rawMessage;
    private final MessageSeverity severity;
    private final MessageType type;
    
    public Message(final RawMessage raw, final MessageClassifier<MessageType> typeClassifier, final MessageClassifier<MessageSeverity> severityClassifier) {
        this.rawMessage = raw;
        this.severity = severityClassifier.classify(raw);
        this.type = typeClassifier.classify(raw);
    }

    public RawMessage getRawMessage() {
        return rawMessage;
    }

    public MessageSeverity getSeverity() {
        return severity;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rawMessage == null) ? 0 : rawMessage.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Message other = (Message) obj;
        if (rawMessage == null) {
            if (other.rawMessage != null)
                return false;
        } else if (!rawMessage.equals(other.rawMessage))
            return false;
        return true;
    }
    
}
