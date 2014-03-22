package com.github.triceo.splitlog;

import java.util.LinkedList;
import java.util.List;

import com.github.triceo.splitlog.splitters.TailSplitter;

final public class MessageBuilder {

    private final List<String> lines = new LinkedList<String>();
    private final Message previousMessage;

    /**
     * Construct a message builder that will link to no previous message.
     * 
     * @param firstLine
     *            Untreated, unprocessed first line retrieved from the log.
     */
    public MessageBuilder(final String firstLine) {
        this(firstLine, null);
    }

    /**
     * Construct a message builder that will link to a particular previous
     * message.
     * 
     * @param firstLine
     *            Untreated, unprocessed first line of the new message retrieved
     *            from the log.
     * @param previousMessage
     *            Previous message, or null if none.
     */
    public MessageBuilder(final String firstLine, final Message previousMessage) {
        if (firstLine == null) {
            throw new IllegalArgumentException("First line may not be null.");
        }
        this.previousMessage = previousMessage;
        this.lines.add(firstLine);
    }

    public Message buildIntermediate(final TailSplitter splitter) {
        return new Message(this.lines, splitter, this.previousMessage);
    }

    public Message buildFinal(final TailSplitter splitter) {
        return new Message(this.lines, splitter, this.previousMessage);
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
