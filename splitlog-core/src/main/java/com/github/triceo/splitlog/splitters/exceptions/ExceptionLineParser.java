package com.github.triceo.splitlog.splitters.exceptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for a particular line type.
 * 
 * @param <T>
 *            The type of line.
 */
abstract class ExceptionLineParser<T extends ExceptionLine> {

    protected static final String JAVA_FQN_REGEX = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*";

    protected Matcher getMatcher(final String regex, final CharSequence line) {
        final Pattern p = Pattern.compile(regex);
        return p.matcher(line);
    }

    public abstract T parse(String line);

}
