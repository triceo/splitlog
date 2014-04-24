package com.github.triceo.splitlog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.ExceptionDescriptor;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageSeverity;
import com.github.triceo.splitlog.api.MessageType;
import com.github.triceo.splitlog.api.TailSplitter;
import com.github.triceo.splitlog.splitters.SimpleTailSplitter;

final class DefaultMessage implements Message {

    private static final TailSplitter DEFAULT_SPLITTER = new SimpleTailSplitter();

    private final ExceptionDescriptor exceptionDescriptor;
    private final List<String> lines;
    private final String logger;
    private final long millisecondsSinceJanuary1st1970;
    private final WeakReference<Message> previousMessage;
    private final MessageSeverity severity;
    private final TailSplitter splitter;
    private final MessageType type;
    private final long uniqueId;

    /**
     * Will call {@link #DefaultMessage(long, Collection, TailSplitter)} with
     * {@link #DEFAULT_SPLITTER}.
     *
     * @param id
     *            Unique ID for the message. No other instance may have this ID,
     *            or else they will be considered equal.
     * @param raw
     *            DefaultMessage lines, expected without any pre-processing.
     */
    protected DefaultMessage(final long id, final Collection<String> raw) {
        this(id, raw, DefaultMessage.DEFAULT_SPLITTER);
    }

    /**
     * Form a new message and infer its metadata using a given splitter.
     *
     * @param id
     *            Unique ID for the message. No other instance may have this ID,
     *            or else they will be considered equal.
     * @param raw
     *            DefaultMessage lines, expected without any pre-processing.
     * @param timestamp
     *            In milliseconds since January 1st 1970. Will be overriden if
     *            {@link TailSplitter} can decode the timestamp from the log.
     * @param splitter
     *            Used to extract metadata out of the raw lines.
     * @param previousMessage
     *            DefaultMessage that preceded this one in the log file. Should
     *            not include tags from {@link CommonFollower}.
     */
    protected DefaultMessage(final long id, final Collection<String> raw, final long timestamp,
        final TailSplitter splitter, final Message previousMessage) {
        if ((raw == null) || raw.isEmpty()) {
            throw new IllegalArgumentException("DefaultMessage must not be null.");
        } else if (splitter == null) {
            throw new IllegalArgumentException("DefaultMessage requires a TailSplitter.");
        }
        if (previousMessage == null) {
            this.previousMessage = null;
        } else {
            this.previousMessage = new WeakReference<Message>(previousMessage);
        }
        this.uniqueId = id;
        this.splitter = splitter;
        this.lines = Collections.unmodifiableList(new ArrayList<String>(raw));
        final String logger = splitter.determineLogger(this.lines);
        this.logger = (logger == null) ? "" : logger;
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
     * Will call
     * {@link #DefaultMessage(long, Collection, long, TailSplitter, DefaultMessage)}
     * with current time and no previous message.
     *
     * @param id
     *            Unique ID for the message. No other instance may have this ID,
     *            or else they will be considered equal.
     * @param raw
     *            DefaultMessage lines, expected without any pre-processing.
     * @param splitter
     *            Used to extract metadata out of the raw lines.
     */
    protected DefaultMessage(final long id, final Collection<String> raw, final TailSplitter splitter) {
        this(id, raw, System.currentTimeMillis(), splitter, null);
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
    protected DefaultMessage(final long id, final String message) {
        if ((message == null) || (message.length() == 0)) {
            throw new IllegalArgumentException("DefaultMessage must not be empty.");
        }
        this.logger = "";
        this.uniqueId = id;
        this.previousMessage = null;
        this.splitter = null;
        this.lines = Collections.singletonList(message.trim());
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
        final DefaultMessage other = (DefaultMessage) obj;
        if (this.uniqueId != other.uniqueId) {
            return false;
        }
        return true;
    }

    @Override
    public Date getDate() {
        return new Date(this.millisecondsSinceJanuary1st1970);
    }

    @Override
    public ExceptionDescriptor getExceptionDescriptor() {
        return this.exceptionDescriptor;
    }

    @Override
    public List<String> getLines() {
        return this.lines;
    }

    @Override
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

    @Override
    public String getLogger() {
        return this.logger;
    }

    @Override
    public Message getPreviousMessage() {
        if (this.previousMessage == null) {
            return null;
        }
        return this.previousMessage.get();
    }

    @Override
    public MessageSeverity getSeverity() {
        return this.severity;
    }

    @Override
    public MessageType getType() {
        return this.type;
    }

    @Override
    public long getUniqueId() {
        return this.uniqueId;
    }

    @Override
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
        sb.append('#');
        sb.append(this.getUniqueId());
        sb.append(" ");
        sb.append(" [");
        sb.append(this.logger);
        sb.append("] ");
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
