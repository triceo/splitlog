package com.github.triceo.splitlog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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

    abstract protected Date determineDate(final RawMessage message);

    abstract protected MessageSeverity determineSeverity(final RawMessage message);

    abstract protected MessageType determineType(final RawMessage message);

    public Message forceProcessing() {
        if (this.lines.size() == 0) {
            return null;
        }
        final RawMessage msg = new RawMessage(this.lines);
        this.lines = new ArrayList<String>();
        return this.processRawMessage(msg);
    }

    abstract protected boolean isStartingLine(final String line);

    private Message processRawMessage(final RawMessage msg) {
        final Collection<String> strippedLines = new ArrayList<String>();
        for (final String line : msg.getLines()) {
            strippedLines.add(this.stripOfMetadata(line));
        }
        return new Message(strippedLines, this.determineDate(msg), this.determineType(msg), this.determineSeverity(msg));
    }

    abstract protected String stripOfMetadata(final String line);

}
