package com.github.triceo.splitlog.parsers.pattern;

public final class Literal implements PatternPart {

    private final String sequence;

    public Literal(final char literal) {
        this.sequence = String.valueOf(literal);
    }

    public Literal(final String literal) {
        this.sequence = literal;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.LOGGER_PROVIDED;
    }

    public String getSequence() {
        return this.sequence;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Literal [");
        if (this.sequence != null) {
            builder.append("sequence='").append(this.sequence).append("'");
        }
        builder.append("]");
        return builder.toString();
    }

}
