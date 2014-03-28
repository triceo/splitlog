package com.github.triceo.splitlog.splitters.exceptions;

import java.net.ConnectException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.ExceptionDescriptor;
import com.github.triceo.splitlog.splitters.exceptions.DefaultExceptionDescriptor;

public class DefaultExceptionDescriptorTest {

    private void assertException(final ExceptionDescriptor ex, final String expectedClassName,
        final String expectedMessage) {
        Assertions.assertThat(ex).isNotNull();
        Assertions.assertThat(ex.getExceptionClassName()).isEqualTo(expectedClassName);
        Assertions.assertThat(ex.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    public void testMaven() {
        final ExceptionDescriptor ex = DefaultExceptionDescriptor.parseStackTrace((ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("exception-maven.txt"))));
        this.assertException(
                ex,
                "java.lang.IllegalStateException",
                "There is no context available for qualifier org.jboss.arquillian.drone.api.annotation.Default. Available contexts are [].");
        Assertions.assertThat(ex.getExceptionClass()).isEqualTo(IllegalStateException.class);
        Assertions.assertThat(ex.getCause()).isNull();
    }

    @Test
    public void testWithCauses() {
        final ExceptionDescriptor ex = DefaultExceptionDescriptor.parseStackTrace((ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("exception-with-causes.txt"))));
        this.assertException(ex, "org.jboss.qa.bpms.rest.wb.RequestFailureException",
                "java.net.ConnectException: Connection refused");
        Assertions.assertThat(ex.getExceptionClass()).isNull(); // highly likely
        final ExceptionDescriptor cause = ex.getCause();
        this.assertException(cause, "java.net.ConnectException", "Connection refused");
        Assertions.assertThat(cause.getExceptionClass()).isEqualTo(ConnectException.class);
        Assertions.assertThat(cause.getCause()).isNull();

    }

    @Test
    public void testWithCauses2() {
        final ExceptionDescriptor ex = DefaultExceptionDescriptor.parseStackTrace((ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("exception-with-causes2.txt"))));
        this.assertException(ex, "javax.servlet.ServletException", "Something bad happened");
        Assertions.assertThat(ex.getCause()).isNotNull();
        // TODO could use a bit more validation; but at least it parses
    }

}
