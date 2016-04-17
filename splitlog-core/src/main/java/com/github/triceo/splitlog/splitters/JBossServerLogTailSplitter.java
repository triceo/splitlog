package com.github.triceo.splitlog.splitters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.triceo.splitlog.api.MessageSeverity;
import com.github.triceo.splitlog.api.MessageType;

/**
 * Provides a tail splitter capable of understanding the JBossAS server.log
 * format, specifically the severities and message types.
 */
final public class JBossServerLogTailSplitter extends AbstractTailSplitter {

    private static final int BODY = 11;
    // hh:mm:ss,mmm
    private static final String DATE_SUBPATTERN = "(([01]?[0-9])|(2[0-3])):([0-5][0-9]):([0-5][0-9]),([0-9][0-9][0-9])";
    private static final int HOURS = 2;
    private static final int MILLIS = 7;
    private static final int MINUTES = 5;
    private static final int SECONDS = 6;
    private static final int SEVERITY = 8;
    // we will try to match any severity string
    private static final String SEVERITY_SUBPATTERN = "[A-Z]+";
    private static final int TYPE = 9;
    // will match fully qualified Java class names, or stderr, stdout etc.
    private static final String TYPE_SUBPATTERN = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]+";
    private final Pattern pattern = Pattern.compile("^\\s*(" + JBossServerLogTailSplitter.DATE_SUBPATTERN + ")\\s+("
            + JBossServerLogTailSplitter.SEVERITY_SUBPATTERN + ")\\s+\\[(" + JBossServerLogTailSplitter.TYPE_SUBPATTERN
            + ")\\]\\s+(.+)\\s*");

    @Override
    public Date determineDate(final List<String> raw) {
        final Matcher m = this.pattern.matcher(raw.get(0));
        m.matches();
        final String hours = m.group(JBossServerLogTailSplitter.HOURS);
        final String minutes = m.group(JBossServerLogTailSplitter.MINUTES);
        final String seconds = m.group(JBossServerLogTailSplitter.SECONDS);
        final String millis = m.group(JBossServerLogTailSplitter.MILLIS);
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hours));
        c.set(Calendar.MINUTE, Integer.parseInt(minutes));
        c.set(Calendar.SECOND, Integer.parseInt(seconds));
        c.set(Calendar.MILLISECOND, Integer.parseInt(millis));
        return c.getTime();
    }

    @Override
    public String determineLogger(final List<String> raw) {
        return this.determineLogger(raw.get(0));
    }

    private String determineLogger(final String line) {
        final Matcher m = this.pattern.matcher(line);
        m.matches();
        final String type = m.group(JBossServerLogTailSplitter.TYPE);
        if (type.equals("stderr") || type.equals("stdout")) {
            return null;
        } else {
            return type;
        }
    }

    @Override
    public MessageSeverity determineSeverity(final List<String> raw) {
        final Matcher m = this.pattern.matcher(raw.get(0));
        m.matches();
        final String severity = m.group(JBossServerLogTailSplitter.SEVERITY);
        switch (severity) {
            case "INFO":
                return MessageSeverity.INFO;
            case "DEBUG":
                return MessageSeverity.DEBUG;
            case "WARN":
                return MessageSeverity.WARNING;
            case "ERROR":
                return MessageSeverity.ERROR;
            case "TRACE":
                return MessageSeverity.TRACE;
            default:
                return MessageSeverity.UNKNOWN;
        }
    }

    @Override
    public MessageType determineType(final List<String> raw) {
        return this.determineType(raw.get(0));
    }

    private MessageType determineType(final String line) {
        final Matcher m = this.pattern.matcher(line);
        m.matches();
        final String type = m.group(JBossServerLogTailSplitter.TYPE);
        switch (type) {
            case "stderr":
                return MessageType.STDERR;
            case "stdout":
                return MessageType.STDOUT;
            default:
                return MessageType.LOG;
        }
    }

    @Override
    public boolean isStartingLine(final String line) {
        return this.pattern.matcher(line).matches();
    }

    @Override
    public String stripOfMetadata(final String line) {
        if (this.isStartingLine(line)) {
            final Matcher m = this.pattern.matcher(line);
            m.matches();
            if (this.determineType(line) == MessageType.LOG) {
                return "[" + m.group(JBossServerLogTailSplitter.TYPE) + "] "
                        + m.group(JBossServerLogTailSplitter.BODY).trim();
            } else {
                return m.group(JBossServerLogTailSplitter.BODY).trim();
            }
        } else {
            return line.trim();
        }
    }

}
