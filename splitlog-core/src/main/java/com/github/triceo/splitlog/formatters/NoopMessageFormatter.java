package com.github.triceo.splitlog.formatters;

import com.github.triceo.splitlog.Message;

/**
 * This formatter will output the message in a format as similar to the original
 * as possible. Some whitespace may still be mangled and line endings will be in
 * the format appropriate for the current environment.
 */
public class NoopMessageFormatter implements MessageFormatter {

    public static final MessageFormatter INSTANCE = new NoopMessageFormatter();

    private NoopMessageFormatter() {
        // singleton
    }

    @Override
    public String format(final Message m) {
        final StringBuilder sb = new StringBuilder();
        for (final String line : m.getLines()) { // FIXME raw?
            sb.append(line);
            sb.append(MessageFormatter.LINE_SEPARATOR);
        }
        return sb.toString().trim();
    }
}
