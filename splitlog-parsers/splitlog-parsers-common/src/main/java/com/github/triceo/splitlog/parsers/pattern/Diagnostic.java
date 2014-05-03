package com.github.triceo.splitlog.parsers.pattern;

public class Diagnostic extends FormattedPatternPart {

    private final String defaultValue;
    private final String property;

    public Diagnostic(final Formatting format) {
        this(format, null);
    }

    public Diagnostic(final Formatting format, final String property) {
        this(format, property, null);
    }

    public Diagnostic(final Formatting format, final String property, final String value) {
        super(format, ContentType.USER_PROVIDED);
        this.property = property;
        this.defaultValue = value;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Diagnostic [");
        if (this.property != null) {
            builder.append("property=").append(this.property).append(", ");
        }
        if (this.defaultValue != null) {
            builder.append("defaultValue=").append(this.defaultValue);
        }
        builder.append("]");
        return builder.toString();
    }

}
