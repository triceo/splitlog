package com.github.triceo.splitlog.splitters.exceptions;

import java.util.regex.Matcher;

class CauseParser extends ExceptionLineParser<CauseLine> {

    @Override
    public CauseLine parse(final String line) {
        final String regex = "^(" + ExceptionLineParser.JAVA_FQN_REGEX + ")(: (.*))?$";
        final Matcher m = this.getMatcher(regex, line);
        if (!m.matches()) {
            return null;
        }
        return new CauseLine(m.group(1), m.group(4));
    }

}
