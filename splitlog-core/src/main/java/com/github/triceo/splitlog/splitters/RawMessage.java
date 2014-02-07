package com.github.triceo.splitlog.splitters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class RawMessage {

    private final List<String> lines;

    public RawMessage(final List<String> lines) {
        if ((lines == null) || (lines.size() == 0)) {
            throw new IllegalArgumentException("Message must have at least one line.");
        } else if (lines.contains(null)) {
            throw new IllegalArgumentException("Neither line can be null.");
        }
        this.lines = Collections.unmodifiableList(new ArrayList<String>(lines));
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
        final RawMessage other = (RawMessage) obj;
        if (this.lines == null) {
            if (other.lines != null) {
                return false;
            }
        } else if (!this.lines.equals(other.lines)) {
            return false;
        }
        return true;
    }

    public String getFirstLine() {
        return this.getLines().get(0);
    }

    public List<String> getLines() {
        return this.lines;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.lines == null) ? 0 : this.lines.hashCode());
        return result;
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
