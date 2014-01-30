package com.github.triceo.splitlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RawMessage {

    private final List<String> lines;

    public RawMessage(final List<String> lines) {
        this.lines = Collections.unmodifiableList(new ArrayList<String>(lines));
    }

    public String getFirstLine() {
        return this.getLines().get(0);
    }

    public List<String> getLines() {
        return this.lines;
    }

    @Override
    public String toString() {
        final String newline = System.getProperty("line.separator");
        final StringBuilder sb = new StringBuilder();
        for (final String line : this.lines) {
            sb.append(line);
            sb.append(newline);
        }
        return sb.toString().trim();
    }

}
