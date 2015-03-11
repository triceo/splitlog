package com.github.triceo.splitlog;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.TailSplitter;
import com.github.triceo.splitlog.splitters.SimpleTailSplitter;

final class MessageBuilder {

    private static final TailSplitter DEFAULT_TAIL_SPLITTER = new SimpleTailSplitter();
    private static final AtomicLong MESSAGE_ID_GENERATOR = new AtomicLong(0);
    private static final long NO_MESSAGE_ID_SET = -1;

    private long futureMessageId = MessageBuilder.NO_MESSAGE_ID_SET;
    private final List<String> lines = new LinkedList<String>();
    private Message previousMessage;
    private long timestamp;

    /**
     * Construct a message builder.
     *
     * @param firstLine
     *            Untreated, unprocessed first line of the new message retrieved
     *            from the log.
     */
    public MessageBuilder(final String firstLine) {
        if (firstLine == null) {
            throw new IllegalArgumentException("First line may not be null.");
        }
        this.timestamp = System.currentTimeMillis();
        this.add(firstLine);
    }

    /**
     * Add a bunch of lines that will become part of the message when built.
     *
     * @param lines
     *            Add untreated, unprocessed lines retrieved from the log.
     * @return This.
     */
    public synchronized MessageBuilder add(final Collection<String> lines) {
        this.lines.addAll(lines);
        return this;
    }

    /**
     * Add another line that will become part of the message when built.
     *
     * @param line
     *            Add an untreated, unprocessed line retrieved from the log.
     * @return This.
     */
    public MessageBuilder add(final String line) {
        return this.add(Collections.singletonList(line));
    }

    public Message buildFinal() {
        return this.buildFinal(MessageBuilder.DEFAULT_TAIL_SPLITTER);
    }

    public synchronized Message buildFinal(final TailSplitter splitter) {
        if (this.futureMessageId == MessageBuilder.NO_MESSAGE_ID_SET) {
            // no ID acquired yet
            this.futureMessageId = MessageBuilder.MESSAGE_ID_GENERATOR.getAndIncrement();
        }
        final Message msg = new DefaultMessage(this.futureMessageId, this.getLines(), this.getTimestamp(), splitter,
                this.previousMessage);
        // next message will have to acquire new ID
        this.futureMessageId = MessageBuilder.NO_MESSAGE_ID_SET;
        return msg;
    }

    public Message buildIntermediate() {
        return this.buildIntermediate(MessageBuilder.DEFAULT_TAIL_SPLITTER);
    }

    public synchronized Message buildIntermediate(final TailSplitter splitter) {
        if (this.futureMessageId == MessageBuilder.NO_MESSAGE_ID_SET) {
            // no ID acquired yet
            this.futureMessageId = MessageBuilder.MESSAGE_ID_GENERATOR.getAndIncrement();
        }
        return new DefaultMessage(this.futureMessageId, this.getLines(), this.getTimestamp(), splitter,
                this.previousMessage);
    }

    public synchronized Message buildTag() {
        if (this.futureMessageId == MessageBuilder.NO_MESSAGE_ID_SET) {
            // no ID acquired yet
            this.futureMessageId = MessageBuilder.MESSAGE_ID_GENERATOR.getAndIncrement();
        }
        final Message msg = new DefaultMessage(this.futureMessageId, this.getFirstLine());
        // next message will have to acquire new ID
        this.futureMessageId = MessageBuilder.NO_MESSAGE_ID_SET;
        return msg;
    }

    /**
     *
     * @return First line from the server log, no pre-processing.
     */
    private String getFirstLine() {
        return this.lines.get(0);
    }

    /**
     *
     * @return Raw lines from the server log that have had no pre-processing. This will create a new list every time it
     * is called, so that {@link #lines} can be modified independently of this new collection's iteration.
     */
    private synchronized List<String> getLines() {
        return Collections.unmodifiableList(new LinkedList<String>(this.lines));
    }

    public Message getPreviousMessage() {
        return this.previousMessage;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public MessageBuilder setPreviousMessage(final Message previousMessage) {
        this.previousMessage = previousMessage;
        return this;
    }

    /**
     * Assign a timestamp to this message.
     *
     * @param timestamp
     *            Timestamp to assign to the message; number of millis since
     *            January 1st 1970.
     * @return This.
     */
    public MessageBuilder setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

}
