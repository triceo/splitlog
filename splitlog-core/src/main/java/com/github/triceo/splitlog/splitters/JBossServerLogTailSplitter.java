package com.github.triceo.splitlog.splitters;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.triceo.splitlog.MessageSeverity;
import com.github.triceo.splitlog.MessageType;

/**
 * Provides a tail splitter capable of understanding the JBossAS server.log
 * format, specifically the severities and message types.
 */
final public class JBossServerLogTailSplitter extends AbstractTailSplitter {

    // hh:mm:ss,mmm
    private static final String DATE_SUBPATTERN = "(([01]?[0-9])|2[0-3]):([0-5][0-9]):([0-5][0-9]),([0-9][0-9][0-9])";
    // we will try to match any severity string
    private static final String SEVERITY_SUBPATTERN = "[A-Z]+";
    // will match fully qualified Java class names, or stderr, stdout etc.
    private static final String TYPE_SUBPATTERN = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]+";
    private final Pattern pattern = Pattern.compile("^\\s*(" + JBossServerLogTailSplitter.DATE_SUBPATTERN + ")\\s+("
            + JBossServerLogTailSplitter.SEVERITY_SUBPATTERN + ")\\s+\\[(" + JBossServerLogTailSplitter.TYPE_SUBPATTERN
            + ")\\]\\s+(.+)\\s*");

    @Override
    protected Date determineDate(final RawMessage message) {
        final Matcher m = this.pattern.matcher(message.getFirstLine());
        m.matches();
        final String hours = m.group(3);
        final String minutes = m.group(4);
        final String seconds = m.group(5);
        final String millis = m.group(6);
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hours));
        c.set(Calendar.MINUTE, Integer.valueOf(minutes));
        c.set(Calendar.SECOND, Integer.valueOf(seconds));
        c.set(Calendar.MILLISECOND, Integer.valueOf(millis));
        return c.getTime();
    }

    @Override
    protected MessageSeverity determineSeverity(final RawMessage message) {
        final Matcher m = this.pattern.matcher(message.getFirstLine());
        m.matches();
        final String severity = m.group(7);
        if (severity.equals("INFO")) {
            return MessageSeverity.INFO;
        } else if (severity.equals("DEBUG")) {
            return MessageSeverity.DEBUG;
        } else if (severity.equals("WARN")) {
            return MessageSeverity.WARNING;
        } else if (severity.equals("ERROR")) {
            return MessageSeverity.ERROR;
        } else if (severity.equals("TRACE")) {
            return MessageSeverity.TRACE;
        } else {
            return MessageSeverity.UNKNOWN;
        }
    }

    @Override
    protected MessageType determineType(final RawMessage message) {
        return this.determineType(message.getFirstLine());
    }

    private MessageType determineType(final String line) {
        final Matcher m = this.pattern.matcher(line);
        m.matches();
        final String type = m.group(8);
        if (type.equals("stderr")) {
            return MessageType.STDERR;
        } else if (type.equals("stdout")) {
            return MessageType.STDOUT;
        } else {
            return MessageType.LOG;
        }
    }

    @Override
    protected boolean isStartingLine(final String line) {
        return this.pattern.matcher(line).matches();
    }

    @Override
    protected String stripOfMetadata(final String line) {
        if (this.isStartingLine(line)) {
            final Matcher m = this.pattern.matcher(line);
            m.matches();
            if (this.determineType(line) == MessageType.LOG) {
                return "[" + m.group(8) + "] " + m.group(10).trim();
            } else {
                return m.group(10).trim();
            }
        } else {
            return line.trim();
        }
    }

}
