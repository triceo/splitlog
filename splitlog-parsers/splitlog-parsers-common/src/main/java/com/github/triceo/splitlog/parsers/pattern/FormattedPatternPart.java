package com.github.triceo.splitlog.parsers.pattern;

public abstract class FormattedPatternPart implements PatternPart {

    private final ContentType contentType;
    private final Formatting formatting;

    protected FormattedPatternPart(final Formatting format) {
        this(format, ContentType.USER_PROVIDED);
    }

    protected FormattedPatternPart(final Formatting format, final ContentType contentType) {
        this.formatting = format;
        this.contentType = contentType;
    }

    @Override
    public ContentType getContentType() {
        return this.contentType;
    }

    // FIXME provide methods directly with sensible defaults in case of null
    public Formatting getFormatting() {
        return this.formatting;
    }

}
