package com.github.triceo.splitlog.parsers.pattern;

public class Formatting {

    public static enum Padding {
        LEFT, RIGHT;
    }

    public static enum Truncate {
        BEGINNING, END;
    }

    private final int maxLength;
    private final int minLength;
    private final Padding padding;
    private final Truncate truncate;

    public Formatting() {
        this(-1, -1, Padding.RIGHT, Truncate.END);
    }

    public Formatting(final int minLength, final int maxLength, final Padding padding, final Truncate truncate) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.padding = padding;
        this.truncate = truncate;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    public int getMinLength() {
        return this.minLength;
    }

    public Padding getPadding() {
        return this.padding;
    }

    public Truncate getTruncate() {
        return this.truncate;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Formatting [minLength=").append(this.minLength).append(", maxLength=").append(this.maxLength)
        .append(", ");
        if (this.padding != null) {
            builder.append("padding=").append(this.padding).append(", ");
        }
        if (this.truncate != null) {
            builder.append("truncate=").append(this.truncate);
        }
        builder.append("]");
        return builder.toString();
    }

}
