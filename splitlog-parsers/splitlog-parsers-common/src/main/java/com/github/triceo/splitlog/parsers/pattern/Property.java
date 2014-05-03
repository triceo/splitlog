package com.github.triceo.splitlog.parsers.pattern;

public class Property extends FormattedPatternPart {

    private final String property;

    public Property(final Formatting format) {
        this(format, null);
    }

    public Property(final Formatting format, final String property) {
        super(format, ContentType.USER_PROVIDED);
        this.property = property;
    }

    public String getProperty() {
        return this.property;
    }

}
