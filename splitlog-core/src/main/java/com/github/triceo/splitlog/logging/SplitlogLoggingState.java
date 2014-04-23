package com.github.triceo.splitlog.logging;

/**
 * Possible states that Splitlog's internal logging system can be found in.
 *
 */
enum SplitlogLoggingState {

    /**
     * Splitlog will only log if, at the time logging is requested,
     * {@link SplitlogLoggerFactory#LOGGING_PROPERTY_NAME} system property is
     * set to {@link SplitlogLoggerFactory#ON_STATE}.
     */
    DEFAULT,
    /**
     * Splitlog internal logging will be disabled.
     */
    OFF,
    /**
     * Splitlog internal logging will be enabled, configuration of the appenders
     * etc. is subject to user settings in the usual logger-specific way.
     */
    ON

}
