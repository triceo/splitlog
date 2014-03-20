package com.github.triceo.splitlog;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.github.triceo.splitlog.splitters.TailSplitter;

final public class MessageBuilder {

    private final List<String> lines = new LinkedList<String>();

    public MessageBuilder(final String firstLine) {
        if (firstLine == null) {
            throw new IllegalArgumentException("First line may not be null.");
        }
        this.lines.add(firstLine);
    }

    private static Collection<String> stripLines(final List<String> lines, final TailSplitter splitter) {
        final Collection<String> stripped = new LinkedList<String>();
        for (final String line : lines) {
            stripped.add(splitter.stripOfMetadata(line));
        }
        return stripped;
    }

    public Message buildIntermediate(final TailSplitter splitter) {
        return new Message(MessageBuilder.stripLines(this.lines, splitter), splitter.determineDate(this),
                splitter.determineType(this), splitter.determineSeverity(this), splitter.determineException(this));
    }

    public Message buildFinal(final TailSplitter splitter) {
        return new Message(MessageBuilder.stripLines(this.lines, splitter), splitter.determineDate(this),
                splitter.determineType(this), splitter.determineSeverity(this), splitter.determineException(this));
    }

    public void addLine(final String line) {
        this.lines.add(line);
    }

    public String getFirstLine() {
        return this.getLines().get(0);
    }

    public List<String> getLines() {
        return this.lines;
    }

}
