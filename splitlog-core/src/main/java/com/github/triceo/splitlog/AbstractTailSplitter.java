package com.github.triceo.splitlog;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTailSplitter implements TailSplitter {

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
        if (this.lines.size() == 0) {
            return null;
        }
        final RawMessage msg = new RawMessage(this.lines);
        this.lines.clear();
        return msg;
    }

    abstract protected boolean isStartingLine(final String line);

}
