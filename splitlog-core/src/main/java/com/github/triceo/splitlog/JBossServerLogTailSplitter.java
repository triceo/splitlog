package com.github.triceo.splitlog;

import java.util.ArrayList;
import java.util.List;

// FIXME should probably become abstract, with the isStartingLine an abstract method.
public class JBossServerLogTailSplitter implements TailSplitter {
    
    private final List<String> lines = new ArrayList<String>();
    
    protected boolean isStartingLine(final String line) {
        // FIXME this will treat each line as a new message
        return true;
    }

    @Override
    public RawMessage addLine(String line) {
        final boolean restart = isStartingLine(line);
        if (restart) {
            if (lines.isEmpty()) {
                lines.add(line);
                return forceProcessing();
            } else {
                RawMessage msg = forceProcessing();
                lines.add(line);
                return msg;
            }
        } else {
            lines.add(line);
            return null;
        }
    }

    @Override
    public RawMessage forceProcessing() {
        RawMessage msg = new RawMessage(lines);
        lines.clear();
        return msg;
    }

}
