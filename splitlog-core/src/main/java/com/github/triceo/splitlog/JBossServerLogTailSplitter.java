package com.github.triceo.splitlog;

import java.util.ArrayList;
import java.util.List;

// FIXME should probably become abstract, with the isStartingLine an abstract method.
public class JBossServerLogTailSplitter implements TailSplitter {

    private final List<String> lines = new ArrayList<String>();

    public RawMessage addLine(final String line) {
        final boolean restart = this.isStartingLine(line);
        if (restart) {
            if (this.lines.isEmpty()) {
                this.lines.add(line);
                return this.forceProcessing();
            } else {
                final RawMessage msg = this.forceProcessing();
                this.lines.add(line);
                return msg;
            }
        } else {
            this.lines.add(line);
            return null;
        }
    }

    public RawMessage forceProcessing() {
        final RawMessage msg = new RawMessage(this.lines);
        this.lines.clear();
        return msg;
    }

    protected boolean isStartingLine(final String line) {
        // FIXME this will treat each line as a new message
        return true;
    }

}
