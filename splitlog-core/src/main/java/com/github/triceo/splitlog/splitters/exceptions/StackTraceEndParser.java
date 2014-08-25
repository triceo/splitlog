package com.github.triceo.splitlog.splitters.exceptions;

import java.util.regex.Matcher;

class StackTraceEndParser extends ExceptionLineParser<StackTraceEndLine> {

    @Override
    public StackTraceEndLine parse(final String line) {
        final Matcher m = this.getMatcher("... (\\d+) common frames omitted", line);
        if (!m.matches()) {
            final Matcher m2 = this.getMatcher("... (\\d+) more", line);
            if (!m2.matches()) {
                return null;
            } else {
                return new StackTraceEndLine(Integer.parseInt(m2.group(1)));
            }
        } else {
            return new StackTraceEndLine(Integer.parseInt(m.group(1)));
        }
    }

}
