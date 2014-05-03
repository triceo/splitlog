package com.github.triceo.splitlog.parsers.pattern;

public class MinimalLengthFormattedPatternPart extends FormattedPatternPart {

    private final int minimalLength;

    protected MinimalLengthFormattedPatternPart(final Formatting format) {
        this(format, -1);
    }

    protected MinimalLengthFormattedPatternPart(final Formatting format, final int minimalLength) {
        super(format, ContentType.FQN);
        this.minimalLength = minimalLength;
    }

    /**
     *
     * @return Negative value means unlimited.
     */
    public int getMinimalLength() {
        return this.minimalLength;
    }

}
