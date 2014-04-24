package com.github.triceo.splitlog.logging;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When Splitlog is running inside an app server such as JBoss, its internal
 * logging may be caught up in the server log. If Splitlog is also set up to
 * watch that server log, a vicious cycle is created, with Splitlog reading its
 * own internal messages. The purpose of this class is to prevent that.
 *
 * Splitlog internals will only log if a system property, whose name is
 * specified in {@link #LOGGING_PROPERTY_NAME}, is set to the value specified in
 * {@link #ON_STATE}. Alternatively, you can force Splitlog to log by calling
 * {@link #enableLogging()}.
 *
 * In order to avoid the aforementioned problem, it is imperative that every
 * SLF4J {@link Logger} is requested through {@link #getLogger(Class)} or
 * {@link #getLogger(String)}. Otherwise, we are powerless and the app server
 * needs to be specifically configured to disregard Splitlog logging.
 *
 */
public final class SplitlogLoggerFactory {

    public static final String LOGGING_PROPERTY_NAME = "splitlog.logging";
    private static final AtomicLong messageCounter = new AtomicLong(0);
    public static final String OFF_STATE = "off";
    public static final String ON_STATE = "on";
    private static SplitlogLoggingState state = SplitlogLoggingState.DEFAULT;

    /**
     * Force Splitlog's internal logging to be enabled.
     */
    public synchronized static void enableLogging() {
        if (SplitlogLoggerFactory.state == SplitlogLoggingState.ON) {
            return;
        }
        SplitlogLoggerFactory.messageCounter.set(0);
        SplitlogLoggerFactory.state = SplitlogLoggingState.ON;
        /*
         * intentionally using the original logger so that this message can not
         * be silenced
         */
        LoggerFactory.getLogger(SplitlogLoggerFactory.class).info("Forcibly enabled Splitlog's internal logging.");
    }

    public static Logger getLogger(final Class<?> cls) {
        return (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(), new Class[] { Logger.class },
                new SplitlogLoggerInvocationHandler(LoggerFactory.getLogger(cls)));
    }

    public static Logger getLogger(final String cls) {
        return (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(), new Class[] { Logger.class },
                new SplitlogLoggerInvocationHandler(LoggerFactory.getLogger(cls)));
    }

    /**
     * Purely for testing purposes.
     *
     * @return How many times {@link #increaseMessageCounter()} has been called
     *         since {@link #state} has last changed.
     */
    static long getMessagesSinceLastStateChange() {
        return SplitlogLoggerFactory.messageCounter.get();
    }

    /**
     * Purely for testing purposes.
     * {@link SplitlogLoggerInvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])}
     * will call this every time when {@link #isLoggingEnabled()} is true.
     */
    static void increaseMessageCounter() {
        SplitlogLoggerFactory.messageCounter.incrementAndGet();
    }

    /**
     * Whether or not Splitlog internals will log if logging is requested at
     * this time.
     *
     * @return True if logging, false if not.
     */
    public synchronized static boolean isLoggingEnabled() {
        switch (SplitlogLoggerFactory.state) {
            case ON:
                return true;
            case OFF:
                return false;
            default:
                final String propertyValue = System.getProperty(SplitlogLoggerFactory.LOGGING_PROPERTY_NAME,
                        SplitlogLoggerFactory.OFF_STATE);
                if (propertyValue.equals(SplitlogLoggerFactory.ON_STATE)) {
                    return true;
                } else {
                    return false;
                }
        }
    }

    /**
     * Whether or not Splitlog's internal messages are logged will depend on the
     * value of {@link #LOGGING_PROPERTY_NAME} system property at the time when
     * logging of the message is requested.
     */
    public synchronized static void resetLoggingToDefaultState() {
        final SplitlogLoggingState previousState = SplitlogLoggerFactory.state;
        if (previousState == SplitlogLoggingState.DEFAULT) {
            // nothing to change
            return;
        }
        /*
         * intentionally using the original logger so that this message can not
         * be silenced
         */
        LoggerFactory.getLogger(SplitlogLoggerFactory.class).info(
                "Splitlog's internal logging reset back to property-driven.");
        SplitlogLoggerFactory.state = SplitlogLoggingState.DEFAULT;
        SplitlogLoggerFactory.messageCounter.set(0);
    }

    /**
     * Force Splitlog's internal logging to be disabled.
     */
    public synchronized static void silenceLogging() {
        if (SplitlogLoggerFactory.state == SplitlogLoggingState.OFF) {
            return;
        }
        SplitlogLoggerFactory.state = SplitlogLoggingState.OFF;
        SplitlogLoggerFactory.messageCounter.set(0);
        /*
         * intentionally using the original logger so that this message can not
         * be silenced
         */
        LoggerFactory.getLogger(SplitlogLoggerFactory.class).info("Forcibly disabled Splitlog's internal logging.");
    }

}
