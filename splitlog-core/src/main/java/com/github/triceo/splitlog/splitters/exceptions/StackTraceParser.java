package com.github.triceo.splitlog.splitters.exceptions;

import java.util.regex.Matcher;

class StackTraceParser extends ExceptionLineParser<StackTraceLine> {

    @Override
    public StackTraceLine parse(final String line) {
        final String regex = "^at (" + ExceptionLineParser.JAVA_FQN_REGEX + ")\\((.*)\\)( \\~?\\[(.*)\\])?$";
        final Matcher m = this.getMatcher(regex, line);
        if (!m.matches()) {
            return null;
        }
        final String methodName = m.group(1);
        final String lineIdentifier = m.group(3);
        final boolean providesJarIdentification = m.group(5) != null;
        final String[] jarIdentificationParts = providesJarIdentification ? m.group(5).split("\\Q:\\E")
                : new String[] {};
        if (providesJarIdentification && (jarIdentificationParts.length != 2)) {
            return null;
        }
        switch (lineIdentifier) {
            case "Unknown Source":
                if (providesJarIdentification) {
                    return StackTraceLine.newUnknownMethod(methodName, jarIdentificationParts);
                } else {
                    return StackTraceLine.newUnknownMethod(methodName);
                }
            case "Native Method":
                if (providesJarIdentification) {
                    return StackTraceLine.newNativeMethod(methodName, jarIdentificationParts);
                } else {
                    return StackTraceLine.newNativeMethod(methodName);
                }
            default:
                final String[] parts = lineIdentifier.split("\\Q:\\E");
                if ((parts.length != 2) || !parts[1].matches("\\d+")) {
                    // not a line number
                    return null;
                }
                if (providesJarIdentification) {
                    return StackTraceLine
                            .newMethod(methodName, parts[0], Integer.parseInt(parts[1]), jarIdentificationParts);
                } else {
                    return StackTraceLine.newMethod(methodName, parts[0], Integer.parseInt(parts[1]));
                }
        }
    }

}
