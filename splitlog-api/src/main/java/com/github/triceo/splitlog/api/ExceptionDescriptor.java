package com.github.triceo.splitlog.api;

import java.util.List;

/**
 * Describes an exception instance.
 *
 */
public interface ExceptionDescriptor {

    /**
     * Returns the cause for this exception.
     *
     * @return Exception data if {@link #isRootCause()} returns true, null
     *         otherwise.
     */
    ExceptionDescriptor getCause();

    /**
     * Best-effort attempt to provide an exception type for
     * {@link #getExceptionClassName()}.
     *
     * @return Exception type if found on classpath, null otherwise.
     */
    Class<? extends Throwable> getExceptionClass();

    String getExceptionClassName();

    String getMessage();

    /**
     * This method will create a brand new unmodifiable list every time it is
     * called. Use with caution.
     *
     * @return Unmodifiable representation of the stack trace.
     */
    List<StackTraceElement> getStackTrace();

    boolean isRootCause();

}
