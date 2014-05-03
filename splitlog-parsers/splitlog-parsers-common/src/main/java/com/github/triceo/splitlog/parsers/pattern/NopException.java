package com.github.triceo.splitlog.parsers.pattern;

public class NopException extends FormattedPatternPart {

    public NopException(final Formatting format) {
        super(format, ContentType.META);
    }

}
