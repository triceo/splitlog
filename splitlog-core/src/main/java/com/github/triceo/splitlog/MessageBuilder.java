package com.github.triceo.splitlog;

import java.util.LinkedList;
import java.util.List;

import com.github.triceo.splitlog.splitters.TailSplitter;

final public class MessageBuilder {

    private final List<String> lines = new LinkedList<String>();
    private final Message previousMessage;
    private long timestamp;

    /**
     * Construct a message builder that will link to no previous message. The
     * message will be timestamped to now.
     * 
     * @param firstLine
     *            Untreated, unprocessed first line retrieved from the log.
     */
    public MessageBuilder(final String firstLine) {
        this(firstLine, System.currentTimeMillis(), null);
    }

    /**
     * Construct a message builder that will link to no previous message.
     * 
     * @param firstLine
     *            Untreated, unprocessed first line retrieved from the log.
     * @param timestamp
     *            Timestamp to assign to the message; number of millis since
     *            January 1st 1970.
     */
    public MessageBuilder(final String firstLine, final long timestamp) {
        this(firstLine, timestamp, null);
    }

    /**
     * Construct a message builder that will link to a particular previous
     * message.
     * 
     * @param firstLine
     *            Untreated, unprocessed first line of the new message retrieved
     *            from the log.
     * @param timestamp
     *            Timestamp to assign to the message; number of millis since
     *            January 1st 1970.
     * @param previousMessage
     *            Previous message, or null if none.
     */
    public MessageBuilder(final String firstLine, final long timestamp, final Message previousMessage) {
        if (firstLine == null) {
            throw new IllegalArgumentException("First line may not be null.");
        }
        this.timestamp = timestamp;
        this.previousMessage = previousMessage;
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
     */
    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public Message getPreviousMessage() {
        return this.previousMessage;
    }

    public Message buildIntermediate(final TailSplitter splitter) {
        return this.buildFinal(splitter);
    }

    public Message buildFinal(final TailSplitter splitter) {
        return new Message(this.getLines(), this.getTimestamp(), splitter, this.previousMessage);
    }

    /**
     * Add another line that will become part of the message when built.
     * 
     * @param line
     *            Add an untreated, unprocessed line retrieved from the log.
     */
    public void addLine(final String line) {
        this.lines.add(line);
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
