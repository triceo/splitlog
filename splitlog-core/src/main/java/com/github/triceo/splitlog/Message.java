package com.github.triceo.splitlog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.github.triceo.splitlog.exceptions.ExceptionDescriptor;
import com.github.triceo.splitlog.splitters.SimpleTailSplitter;
import com.github.triceo.splitlog.splitters.TailSplitter;

/**
 * A set of lines from the watched file, that is likely to constitute a single
 * log message.
 */
final public class Message {

    private static final TailSplitter DEFAULT_SPLITTER = new SimpleTailSplitter();

    private final long uniqueId;
    private final TailSplitter splitter;
    private final List<String> lines;
    private final MessageSeverity severity;
    private final MessageType type;
    private final WeakReference<Message> previousMessage;
    private final long millisecondsSinceJanuary1st1970;
    private final ExceptionDescriptor exceptionDescriptor;

    /**
     * Will call {@link #Message(long, Collection, TailSplitter)} with
     * {@link #DEFAULT_SPLITTER}.
     * 
     * @param id
     *            Unique ID for the message. No other instance may have this ID,
     *            or else they will be considered equal.
     * @param raw
     *            Message lines, expected without any pre-processing.
     */
    protected Message(final long id, final Collection<String> raw) {
        this(id, raw, Message.DEFAULT_SPLITTER);
    }

    /**
     * Will call {@link #Message(long, Collection, long, TailSplitter, Message)}
     * with current time and no previous message.
     * 
     * @param id
     *            Unique ID for the message. No other instance may have this ID,
     *            or else they will be considered equal.
     * @param raw
     *            Message lines, expected without any pre-processing.
     * @param splitter
     *            Used to extract metadata out of the raw lines.
     */
    protected Message(final long id, final Collection<String> raw, final TailSplitter splitter) {
        this(id, raw, System.currentTimeMillis(), splitter, null);
    }

    /**
     * Form a new message and infer its metadata using a given splitter.
     * 
     * @param id
     *            Unique ID for the message. No other instance may have this ID,
     *            or else they will be considered equal.
     * @param raw
     *            Message lines, expected without any pre-processing.
     * @param timestamp
     *            In milliseconds since January 1st 1970. Will be overriden if
     *            {@link TailSplitter} can decode the timestamp from the log.
     * @param splitter
     *            Used to extract metadata out of the raw lines.
     * @param previousMessage
     *            Message that preceded this one in the log file. Should not
     *            include tags from {@link CommonFollower}.
     */
    protected Message(final long id, final Collection<String> raw, final long timestamp, final TailSplitter splitter,
            final Message previousMessage) {
        if ((raw == null) || raw.isEmpty()) {
            throw new IllegalArgumentException("Message must not be null.");
        } else if (splitter == null) {
            throw new IllegalArgumentException("Message requires a TailSplitter.");
        }
        if (previousMessage == null) {
            this.previousMessage = null;
        } else {
            this.previousMessage = new WeakReference<Message>(previousMessage);
        }
        this.uniqueId = id;
        this.splitter = splitter;
        this.lines = Collections.unmodifiableList(new ArrayList<String>(raw));
        this.severity = splitter.determineSeverity(this.lines);
        this.type = splitter.determineType(this.lines);
        this.exceptionDescriptor = splitter.determineException(this.lines);
        // determine message timestamp
        final Date d = splitter.determineDate(this.lines);
        if (d == null) {
            this.millisecondsSinceJanuary1st1970 = timestamp;
        } else {
            this.millisecondsSinceJanuary1st1970 = d.getTime();
        }
    }

    /**
     * Return a message that preceded this one in the same log stream.
     * 
     * @return Null if there was no such message, the message already got GC'd,
     *         or <code>{@link #getType()} == {@link MessageType#TAG}</code>.
     */
    public Message getPreviousMessage() {
        if (this.previousMessage == null) {
            return null;
        }
        return this.previousMessage.get();
    }

    /**
     * Creates a one-line message of type {@link MessageType#TAG}. The message
     * will point to no previous message.
     * 
     * @param id
     *            Unique ID for the message. No other instance may have this ID,
     *            or else they will be considered equal.
     * @param message
     *            The only line in the message.
     */
    protected Message(final long id, final String message) {
        if ((message == null) || (message.length() == 0)) {
            throw new IllegalArgumentException("Message must not be empty.");
        }
        this.uniqueId = id;
        this.previousMessage = null;
        this.splitter = null;
        this.lines = Collections.singletonList(message.trim());
        this.severity = MessageSeverity.UNKNOWN;
        this.type = MessageType.TAG;
        this.millisecondsSinceJanuary1st1970 = System.currentTimeMillis();
        this.exceptionDescriptor = null;
    }

    /**
     * Two {@link Message}s are equal if and only if they have the same
     * {@link #getUniqueId()}. This effectively means no two distinct ones will
     * ever be equal.
     */
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
        if (this.uniqueId != other.uniqueId) {
            return false;
        }
        return true;
    }

    /**
     * Unique ID of the message, that can be used to compare messages in the
     * order of their arrival into this tool.
     * 
     * @return ID of the message, guaranteed to be unique for every message, and
     *         increasing from message to message.
     */
    public long getUniqueId() {
        return this.uniqueId;
    }

    /**
     * Get the date that this message was logged on.
     * 
     * @return Newly constructed instance of {@link Date} with the message log
     *         timestamp.
     */
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
     * Get each line of the message.
     * 
     * @return Unmodifiable representation of lines in this message, exactly as
     *         were received.
     */
    public List<String> getLines() {
        return this.lines;
    }

    /**
     * Get each line of the message, with metadata stripped out.
     * 
     * @return Unmodifiable representation of text of the message.
     */
    public List<String> getLinesWithoutMetadata() {
        if (this.type == MessageType.TAG) {
            // nothing to split for tags
            return this.lines;
        }
        final List<String> stripped = new ArrayList<String>();
        for (final String line : this.lines) {
            stripped.add(this.splitter.stripOfMetadata(line));
        }
        return Collections.unmodifiableList(stripped);
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
        result = (prime * result) + (int) (this.uniqueId ^ (this.uniqueId >>> 32));
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
        sb.append(" '");
        sb.append(this.lines.get(0));
        sb.append("'");
        if (this.lines.size() > 1) {
            sb.append(" and ");
            sb.append(this.lines.size() - 1);
            sb.append(" more lines...");
        }
        return sb.toString();
    }

}
