package com.github.triceo.splitlog.exceptions;

public class ExceptionParseException extends Exception {

    private static final long serialVersionUID = 5762117433604515469L;

    public ExceptionParseException(final String offendingLine, final String message) {
        super("Failed parsing exception. Reason given: '" + message + "'. Offending line: '" + offendingLine + "'.");
    }

}
