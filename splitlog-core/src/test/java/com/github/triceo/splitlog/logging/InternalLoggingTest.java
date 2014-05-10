package com.github.triceo.splitlog.logging;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.AbstractSplitlogTest;

public class InternalLoggingTest extends AbstractSplitlogTest {

    /**
     * Purpose of this test is to make sure that, if the property is not set,
     * the logging is disabled by default.
     */
    @Test
    public void testDefaultValue() {
        // the default will be set by surefire to "on"; reset that
        final String defaultValue = System.getProperty(SplitlogLoggerFactory.LOGGING_PROPERTY_NAME,
                SplitlogLoggerFactory.OFF_STATE);
        Assertions.assertThat(defaultValue).isEqualTo(SplitlogLoggerFactory.ON_STATE);
        System.getProperties().remove(SplitlogLoggerFactory.LOGGING_PROPERTY_NAME);
        // and now check the default default :-)
        final boolean isEnabled = SplitlogLoggerFactory.isLoggingEnabled();
        /*
         * but before that, restore the original; if the assertion at the end
         * fails, we want the original state for the rest of the tests
         */
        System.setProperty(SplitlogLoggerFactory.LOGGING_PROPERTY_NAME, defaultValue);
        Assertions.assertThat(isEnabled).isFalse();
    }

    @Test
    public void testLogging() {
        final boolean defaultValue = SplitlogLoggerFactory.isLoggingEnabled();
        SplitlogLoggerFactory.enableLogging();
        Assertions.assertThat(SplitlogLoggerFactory.isLoggingEnabled()).isTrue();
        Assertions.assertThat(SplitlogLoggerFactory.getMessagesSinceLastStateChange()).isEqualTo(0);
        SplitlogLoggerFactory.getLogger(InternalLoggingTest.class).error("This will be logged.");
        final long logged = SplitlogLoggerFactory.getMessagesSinceLastStateChange();
        SplitlogLoggerFactory.resetLoggingToDefaultState();
        Assertions.assertThat(SplitlogLoggerFactory.isLoggingEnabled()).isEqualTo(defaultValue);
        Assertions.assertThat(logged).isEqualTo(1);
    }

    @Test
    public void testSilence() {
        final boolean defaultValue = SplitlogLoggerFactory.isLoggingEnabled();
        SplitlogLoggerFactory.silenceLogging();
        Assertions.assertThat(SplitlogLoggerFactory.isLoggingEnabled()).isFalse();
        Assertions.assertThat(SplitlogLoggerFactory.getMessagesSinceLastStateChange()).isEqualTo(0);
        SplitlogLoggerFactory.getLogger(InternalLoggingTest.class).error("This must not be logged.");
        final long logged = SplitlogLoggerFactory.getMessagesSinceLastStateChange();
        SplitlogLoggerFactory.resetLoggingToDefaultState();
        Assertions.assertThat(SplitlogLoggerFactory.isLoggingEnabled()).isEqualTo(defaultValue);
        Assertions.assertThat(logged).isEqualTo(0);
    }

}
