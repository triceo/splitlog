package com.github.triceo.splitlog.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that describes an exception instance.
 * 
 */
public class ExceptionDescriptor {

    private static class Builder {

        private final Map<CauseLine, List<StackTraceElement>> causes = new LinkedHashMap<CauseLine, List<StackTraceElement>>();
        private List<StackTraceElement> currentlyUsedStackTrace;

        public void addLine(final ExceptionLine line) {
            if (line instanceof CauseLine) {
                // start a new exception
                this.causes.put((CauseLine) line, new ArrayList<StackTraceElement>());
                this.currentlyUsedStackTrace = this.causes.get(line);
            } else if (line instanceof StackTraceLine) {
                // add a stack trace element to the current exception
                final StackTraceLine l = (StackTraceLine) line;
                final String fqMethodName = l.getMethodName();
                final int index = fqMethodName.lastIndexOf('.');
                final StackTraceElement e = new StackTraceElement(fqMethodName.substring(0, index),
                        fqMethodName.substring(index + 1), l.getClassName(), l.getLineInCode());
                this.currentlyUsedStackTrace.add(e);
            } else {
                // we don't care about any other kind of information
            }
        }

        public ExceptionDescriptor build() {
            final List<CauseLine> properlyOrdered = new ArrayList<CauseLine>(this.causes.keySet());
            ExceptionDescriptor previousException = null;
            for (int i = properlyOrdered.size() - 1; i >= 0; i--) {
                final CauseLine cause = properlyOrdered.get(i);
                final List<StackTraceElement> stackTrace = this.causes.get(cause);
                previousException = new ExceptionDescriptor(cause.getClassName(), cause.getMessage(), stackTrace,
                        previousException);
            }
            return previousException;
        }

    }

    /**
     * Take a chunk of log and try to parse an exception out of it.
     * 
     * @param lines
     *            Any random log, separated into lines.
     * @return First exception found, including full stack trace with causes, or
     *         null if none identified.
     */
    public static ExceptionDescriptor parseStackTrace(final Collection<String> lines) {
        if ((lines == null) || lines.isEmpty()) {
            throw new IllegalArgumentException("No stack trace provided.");
        }
        try {
            final Builder b = new Builder();
            final Collection<ExceptionLine> parsedLines = new ExceptionParser().parse(lines);
            for (final ExceptionLine line : parsedLines) {
                b.addLine(line);
            }
            return b.build();
        } catch (final ExceptionParseException e) {
            // FIXME provides no information as to why there's no exception
            // found
            return null;
        }
    }

    private final List<StackTraceElement> stackTrace;

    private final String exceptionClassName, message;
    private final ExceptionDescriptor cause;

    private ExceptionDescriptor(final String className, final String message, final List<StackTraceElement> elements,
            final ExceptionDescriptor cause) {
        this.exceptionClassName = className;
        this.message = message;
        this.stackTrace = Collections.unmodifiableList(elements);
        this.cause = cause;
    }

    /**
     * Returns the cause for this exception.
     * 
     * @return Exception data if {@link #isRootCause()} returns true, null
     *         otherwise.
     */
    public ExceptionDescriptor getCause() {
        return this.cause;
    }

    /**
     * Best-effort attempt to provide an exception type for
     * {@link #getExceptionClassName()}.
     * 
     * @return Exception type if found on classpath, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Throwable> getExceptionClass() {
        try {
            return (Class<? extends Throwable>) Class.forName(this.getExceptionClassName());
        } catch (final ClassNotFoundException e) {
            // the exception in the log came from code that is not on known here
            return null;
        }
    }

    public String getExceptionClassName() {
        return this.exceptionClassName;
    }

    public String getMessage() {
        return this.message;
    }

    public List<StackTraceElement> getStackTrace() {
        return this.stackTrace;
    }

    public boolean isRootCause() {
        return this.cause == null;
    }

    @Override
    public String toString() {
        final StringBuilder builder2 = new StringBuilder();
        builder2.append("ExceptionDescriptor [");
        if (this.stackTrace != null) {
            builder2.append("stackTrace=").append(this.stackTrace).append(", ");
        }
        if (this.exceptionClassName != null) {
            builder2.append("exceptionClassName=").append(this.exceptionClassName).append(", ");
        }
        if (this.message != null) {
            builder2.append("message=").append(this.message).append(", ");
        }
        if (this.cause != null) {
            builder2.append("cause=").append(this.cause);
        }
        builder2.append("]");
        return builder2.toString();
    }

}
