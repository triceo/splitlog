package com.github.triceo.splitlog.parsers.pattern;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class LoggingDate extends FormattedPatternPart {

    private static final String ISO8601 = "ISO8601";

    private final DateFormat dateFormat;
    private final TimeZone timezone;

    public LoggingDate(final Formatting format) {
        this(format, LoggingDate.ISO8601);
    }

    public LoggingDate(final Formatting format, final String dateFormat) {
        this(format, dateFormat, "GMT");
    }

    public LoggingDate(final Formatting format, final String dateFormat, final String timezone) {
        super(format, ContentType.LOGGER_PROVIDED);
        this.dateFormat = new SimpleDateFormat(dateFormat.equals(LoggingDate.ISO8601) ? "yyyy-MM-dd'T'HH:mm'Z'" : dateFormat);
        this.timezone = TimeZone.getTimeZone(timezone);
    }

    public DateFormat getDateFormat() {
        return this.dateFormat;
    }

    public TimeZone getTimezone() {
        return this.timezone;
    }

}
