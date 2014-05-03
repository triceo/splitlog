package com.github.triceo.splitlog.parsers.pattern;

public class Payload extends FormattedPatternPart {

    public Payload(final Formatting format) {
        super(format, ContentType.USER_PROVIDED);
    }

}
