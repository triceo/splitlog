package com.github.triceo.splitlog.exceptions;

import java.net.ConnectException;

import org.junit.Assert;
import org.junit.Test;

public class ExceptionDescriptorTest {

    private void assertException(final ExceptionDescriptor ex, final String expectedClassName,
        final String expectedMessage) {
        Assert.assertNotNull(ex);
        Assert.assertEquals(expectedClassName, ex.getExceptionClassName());
        Assert.assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    public void testMaven() {
        final ExceptionDescriptor ex = ExceptionDescriptor.parseStackTrace((ExceptionParserTest.parseIntoLines(this
                .getClass().getResourceAsStream("exception-maven.txt"))));
        this.assertException(
                ex,
                "java.lang.IllegalStateException",
                "There is no context available for qualifier org.jboss.arquillian.drone.api.annotation.Default. Available contexts are [].");
        Assert.assertEquals(IllegalStateException.class, ex.getExceptionClass());
        Assert.assertNull(ex.getCause());
    }

    @Test
    public void testWithCauses() {
        final ExceptionDescriptor ex = ExceptionDescriptor.parseStackTrace((ExceptionParserTest.parseIntoLines(this
                .getClass().getResourceAsStream("exception-with-causes.txt"))));
        this.assertException(ex, "org.jboss.qa.bpms.rest.wb.RequestFailureException",
                "java.net.ConnectException: Connection refused");
        Assert.assertNull(ex.getExceptionClass()); // highly likely
        final ExceptionDescriptor cause = ex.getCause();
        this.assertException(cause, "java.net.ConnectException", "Connection refused");
        Assert.assertEquals(ConnectException.class, cause.getExceptionClass());
        Assert.assertNull(cause.getCause());

    }

    @Test
    public void testWithCauses2() {
        final ExceptionDescriptor ex = ExceptionDescriptor.parseStackTrace((ExceptionParserTest.parseIntoLines(this
                .getClass().getResourceAsStream("exception-with-causes2.txt"))));
        this.assertException(ex, "javax.servlet.ServletException", "Something bad happened");
        Assert.assertNotNull(ex.getCause());
        // TODO could use a bit more validation; but at least it parses
    }

}
