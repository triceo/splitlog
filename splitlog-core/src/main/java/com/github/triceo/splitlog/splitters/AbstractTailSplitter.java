package com.github.triceo.splitlog.splitters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.github.triceo.splitlog.Message;
import com.github.triceo.splitlog.MessageSeverity;
import com.github.triceo.splitlog.MessageType;

public abstract class AbstractTailSplitter implements TailSplitter {

    private List<String> lines = new ArrayList<String>();

    public Message addLine(final String line) {
        final boolean restart = this.isStartingLine(line);
        if (restart) {
            if (this.lines.isEmpty()) {
                this.lines.add(line);
                return null;
            } else {
                final Message msg = this.forceProcessing();
                this.lines.add(line);
                return msg;
            }
        } else {
            /*
             * we need to make sure that we are only recording from the first
             * starting line.
             */
            if (!this.lines.isEmpty()) {
                this.lines.add(line);
            }
            return null;
        }
    }

    /**
     * Read the message and find the date when the message was submitted.
     * 
     * @param message
     *            Message in question.
     * @return Date from the message, or the current date if not found.
     */
    abstract protected Date determineDate(final RawMessage message);

    /**
     * Read the message and try to find its severity.
     * 
     * @param message
     *            Message in question.
     * @return Severity included in the message, {@link MessageSeverity#UNKNOWN}
     *         otherwise.
     */
    abstract protected MessageSeverity determineSeverity(final RawMessage message);

    /**
     * Read the message and try to find its type.
     * 
     * @param message
     *            Message in question.
     * @return Type guessed from the message, {@value MessageType#LOG} if
     *         undetermined.
     */
    abstract protected MessageType determineType(final RawMessage message);

    public Message forceProcessing() {
        if (this.lines.size() == 0) {
            return null;
        }
        final RawMessage msg = new RawMessage(this.lines);
        this.lines = new ArrayList<String>();
        return this.processRawMessage(msg);
    }

    /**
     * Whether or not this particular line from the log starts a new log
     * message. If so, it will be used to trigger the return of an actual
     * Message instance from {@link #addLine(String)}. This instance will hold
     * the previous message that has been just considered finished.
     * 
     * @param line
     *            Line from the log.
     * @return True if this is the first line of a new log message.
     */
    abstract protected boolean isStartingLine(final String line);

    private Message processRawMessage(final RawMessage msg) {
        final Collection<String> strippedLines = new ArrayList<String>();
        for (final String line : msg.getLines()) {
            strippedLines.add(this.stripOfMetadata(line));
        }
        return new Message(strippedLines, this.determineDate(msg), this.determineType(msg), this.determineSeverity(msg));
    }

    /**
     * Take a line from the log and attempt to strip it of metadata, such as
     * severity, type and date.
     * 
     * @param line
     *            Line from the log.
     * @return The same line, stripped of metadata. Usually just the actual
     *         information being logged.
     */
    abstract protected String stripOfMetadata(final String line);

}
