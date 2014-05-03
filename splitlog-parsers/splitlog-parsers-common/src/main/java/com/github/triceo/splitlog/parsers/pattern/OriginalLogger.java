package com.github.triceo.splitlog.parsers.pattern;

public class OriginalLogger extends MinimalLengthFormattedPatternPart {

    public OriginalLogger(final Formatting format) {
        super(format);
    }

    public OriginalLogger(final Formatting format, final int minimalLength) {
        super(format, minimalLength);
    }

}
