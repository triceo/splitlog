package com.github.triceo.splitlog;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.github.triceo.splitlog.splitters.SimpleTailSplitter;
import com.github.triceo.splitlog.splitters.TailSplitter;

final public class MessageBuilder {

    private static final TailSplitter DEFAULT_TAIL_SPLITTER = new SimpleTailSplitter();
    private static final long NO_MESSAGE_ID_SET = -1;
    private static final AtomicLong MESSAGE_ID_GENERATOR = new AtomicLong(0);

    private final List<String> lines = new LinkedList<String>();
    private Message previousMessage;
    private long timestamp;
    private long futureMessageId = MessageBuilder.NO_MESSAGE_ID_SET;

    public MessageBuilder setPreviousMessage(final Message previousMessage) {
        this.previousMessage = previousMessage;
        return this;
    }

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
        this.lines.add(firstLine);
    }

    public long getTimestamp() {
        return this.timestamp;
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

    public Message getPreviousMessage() {
        return this.previousMessage;
    }

    public synchronized Message buildIntermediate(final TailSplitter splitter) {
        if (this.futureMessageId == MessageBuilder.NO_MESSAGE_ID_SET) {
            // no ID acquired yet
            this.futureMessageId = MessageBuilder.MESSAGE_ID_GENERATOR.getAndIncrement();
        }
        return new Message(this.futureMessageId, this.getLines(), this.getTimestamp(), splitter, this.previousMessage);
    }

    public synchronized Message buildFinal(final TailSplitter splitter) {
        if (this.futureMessageId == MessageBuilder.NO_MESSAGE_ID_SET) {
            // no ID acquired yet
            this.futureMessageId = MessageBuilder.MESSAGE_ID_GENERATOR.getAndIncrement();
        }
        final Message msg = new Message(this.futureMessageId, this.getLines(), this.getTimestamp(), splitter,
                this.previousMessage);
        // next message will have to acquire new ID
        this.futureMessageId = MessageBuilder.NO_MESSAGE_ID_SET;
        return msg;
    }

    public synchronized Message buildFinal() {
        return this.buildFinal(MessageBuilder.DEFAULT_TAIL_SPLITTER);
    }

    public synchronized Message buildIntermediate() {
        return this.buildIntermediate(MessageBuilder.DEFAULT_TAIL_SPLITTER);
    }

    public synchronized Message buildTag() {
        if (this.futureMessageId == MessageBuilder.NO_MESSAGE_ID_SET) {
            // no ID acquired yet
            this.futureMessageId = MessageBuilder.MESSAGE_ID_GENERATOR.getAndIncrement();
        }
        final Message msg = new Message(this.futureMessageId, this.getFirstLine());
        // next message will have to acquire new ID
        this.futureMessageId = MessageBuilder.NO_MESSAGE_ID_SET;
        return msg;
    }

    /**
     * Add another line that will become part of the message when built.
     * 
     * @param line
     *            Add an untreated, unprocessed line retrieved from the log.
     * @return This.
     */
    public MessageBuilder add(final String line) {
        this.lines.add(line);
        return this;
    }

    /**
     * Add a bunch of lines that will become part of the message when built.
     * 
     * @param lines
     *            Add untreated, unprocessed lines retrieved from the log.
     * @return This.
     */
    public MessageBuilder add(final Collection<String> lines) {
        this.lines.addAll(lines);
        return this;
    }

    /**
     * 
     * @return First line from the server log, no pre-processing.
     */
    public String getFirstLine() {
        return this.getLines().get(0);
    }

    /**
     * 
     * @return Raw lines from the server log that have had no pre-processing.
     */
    public List<String> getLines() {
        return this.lines;
    }

}
