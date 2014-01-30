package com.github.triceo.splitlog;

public class DefaultTailSplitter extends AbstractTailSplitter {

    @Override
    protected boolean isStartingLine(final String line) {
        // FIXME this will treat each line as a new message
        return true;
    }

}
