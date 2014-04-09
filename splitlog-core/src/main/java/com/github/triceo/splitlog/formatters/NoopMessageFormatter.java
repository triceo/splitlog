package com.github.triceo.splitlog.formatters;

import java.io.File;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageFormatter;

/**
 * This formatter will output the message in a format as similar to the original
 * as possible. Some whitespace may still be mangled and line endings will be in
 * the format appropriate for the current environment.
 */
public final class NoopMessageFormatter implements MessageFormatter {

    public static final MessageFormatter INSTANCE = new NoopMessageFormatter();

    private NoopMessageFormatter() {
        // singleton
    }

    @Override
    public String format(final Message m, final File source) {
        final StringBuilder sb = new StringBuilder();
        for (final String line : m.getLines()) {
            sb.append(line);
            sb.append(MessageFormatter.LINE_SEPARATOR);
        }
        return sb.toString().trim();
    }
}
