package com.github.triceo.splitlog.logging;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.slf4j.Logger;

/**
 * This is the proxy between {@link Logger} and the calling code. It will make
 * sure that Splitlog doesn't log anything while
 * {@link SplitlogLoggerFactory#isLoggingEnabled()} is false.
 *
 */
final class SplitlogLoggerInvocationHandler implements InvocationHandler {

    private final Logger proxied;

    public SplitlogLoggerInvocationHandler(final Logger logger) {
        this.proxied = logger;
    }

    /**
     * Will call the proxied method, but only if
     * {@link SplitlogLoggerFactory#isLoggingEnabled()} is true at the time.
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (SplitlogLoggerFactory.isLoggingEnabled()) {
            SplitlogLoggerFactory.increaseMessageCounter();
            return method.invoke(this.proxied, args);
        } else {
            // logging is not enabled, don't proxy the call
            return null;
        }
    }

}