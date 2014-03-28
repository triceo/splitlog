package com.github.triceo.splitlog.exceptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.triceo.splitlog.api.ExceptionDescriptor;

/**
 * A class that describes an exception instance.
 * 
 */
public class DefaultExceptionDescriptor implements ExceptionDescriptor {

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
                previousException = new DefaultExceptionDescriptor(cause.getClassName(), cause.getMessage(),
                        stackTrace, previousException);
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
            final Collection<ExceptionLine> parsedLines = ExceptionParser.INSTANCE.parse(lines);
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

    private final ExceptionDescriptor cause;

    private final String exceptionClassName, message;
    private final StackTraceElement[] stackTrace;

    private DefaultExceptionDescriptor(final String className, final String message,
            final List<StackTraceElement> elements, final ExceptionDescriptor cause) {
        this.exceptionClassName = className;
        this.message = message;
        this.stackTrace = elements.toArray(new StackTraceElement[elements.size()]);
        this.cause = cause;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final DefaultExceptionDescriptor other = (DefaultExceptionDescriptor) obj;
        if (this.cause == null) {
            if (other.cause != null) {
                return false;
            }
        } else if (!this.cause.equals(other.cause)) {
            return false;
        }
        if (this.exceptionClassName == null) {
            if (other.exceptionClassName != null) {
                return false;
            }
        } else if (!this.exceptionClassName.equals(other.exceptionClassName)) {
            return false;
        }
        if (this.message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!this.message.equals(other.message)) {
            return false;
        }
        if (!Arrays.equals(this.stackTrace, other.stackTrace)) {
            return false;
        }
        return true;
    }

    @Override
    public ExceptionDescriptor getCause() {
        return this.cause;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Throwable> getExceptionClass() {
        try {
            return (Class<? extends Throwable>) Class.forName(this.getExceptionClassName());
        } catch (final ClassNotFoundException e) {
            // the exception in the log came from code that is not on known here
            return null;
        }
    }

    @Override
    public String getExceptionClassName() {
        return this.exceptionClassName;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public List<StackTraceElement> getStackTrace() {
        return Collections.unmodifiableList(Arrays.asList(this.stackTrace));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.cause == null) ? 0 : this.cause.hashCode());
        result = (prime * result) + ((this.exceptionClassName == null) ? 0 : this.exceptionClassName.hashCode());
        result = (prime * result) + ((this.message == null) ? 0 : this.message.hashCode());
        result = (prime * result) + Arrays.hashCode(this.stackTrace);
        return result;
    }

    @Override
    public boolean isRootCause() {
        return this.cause == null;
    }

    @Override
    public String toString() {
        final StringBuilder builder2 = new StringBuilder();
        builder2.append("ExceptionDescriptor [");
        if (this.stackTrace != null) {
            builder2.append("stackTrace=").append(Arrays.toString(this.stackTrace)).append(", ");
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
