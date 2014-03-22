package com.github.triceo.splitlog.formatters;

import com.github.triceo.splitlog.Message;

/**
 * This formatter will output the message in a format specific to Splitlog. It
 * will strip metadata out of the original message and only include some of them
 * in the new output.
 */
public class UnifyingMessageFormatter implements MessageFormatter {

    public static final MessageFormatter INSTANCE = new UnifyingMessageFormatter();

    private UnifyingMessageFormatter() {
        // singleton
    }

    @Override
    public String format(final Message m) {
        final StringBuilder sb = new StringBuilder();
        sb.append("#");
        sb.append(m.getUniqueId());
        sb.append(" ");
        sb.append(m.getDate());
        sb.append(" (");
        sb.append(m.getType());
        sb.append(") ");
        sb.append(m.getSeverity());
        sb.append(" ");
        for (final String line : m.getLines()) { // FIXME raw?
            sb.append(line);
            sb.append(MessageFormatter.LINE_SEPARATOR);
        }
        return sb.toString().trim();
    }
}
