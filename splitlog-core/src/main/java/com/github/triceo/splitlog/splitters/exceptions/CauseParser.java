package com.github.triceo.splitlog.splitters.exceptions;

import java.util.regex.Matcher;

class CauseParser extends ExceptionLineParser<CauseLine> {

    private static final String SEPARATOR = ":";

    private static String merge(final String[] chunks, final int firstIndex) {
        String result = "";
        for (int i = firstIndex; i < chunks.length; i++) {
            result += chunks[i];
            if (i != (chunks.length - 1)) {
                result += CauseParser.SEPARATOR;
            }
        }
        return result;
    }

    @Override
    public CauseLine parse(final String line) {
        // split the line by ":" and find the first FQN in the result
        final String[] chunks = line.split("\\Q" + CauseParser.SEPARATOR + "\\E");
        if (chunks.length < 2) {
            return null;
        }
        for (int i = 0; i < chunks.length; i++) {
            final String chunk = chunks[i].trim();
            final Matcher m = this.getMatcher("^" + ExceptionLineParser.JAVA_FQN_REGEX + "$", chunk);
            if (m.matches()) {
                return new CauseLine(chunk, CauseParser.merge(chunks, i + 1).trim());
            }
        }
        return null;
    }

}
