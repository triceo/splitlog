package com.github.triceo.splitlog.exceptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.github.triceo.splitlog.exceptions.StackTraceLine.Source;

public class ExceptionParserTest {

    // FIXME share this in a better way
    public static final Collection<String> parseIntoLines(final InputStream s) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(s));
            final Collection<String> lines = new ArrayList<String>();
            String line = null;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (final Exception ex) {
            return Collections.emptyList();
        } finally {
            IOUtils.closeQuietly(r);
        }
    }

    @Test
    public void testExceptionWithCauses() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<ExceptionLine>(
                ExceptionParser.INSTANCE.parse(ExceptionParserTest.parseIntoLines(this.getClass().getResourceAsStream(
                        "exception-with-causes.txt"))));
        Assert.assertEquals(48, lines.size()); // one item per line in the file
        // verify the initial cause
        final CauseLine firstLine = (CauseLine) lines.get(0);
        Assert.assertEquals("org.jboss.qa.bpms.rest.wb.RequestFailureException", firstLine.getClassName());
        Assert.assertEquals("java.net.ConnectException: Connection refused", firstLine.getMessage());
        // verify one native method
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(7);
        Assert.assertEquals(Source.NATIVE, stackTraceLine.getSource());
        Assert.assertEquals("sun.reflect.NativeMethodAccessorImpl.invoke0", stackTraceLine.getMethodName());
        Assert.assertEquals("na", stackTraceLine.getClassSource());
        Assert.assertEquals("1.7.0_51", stackTraceLine.getClassSourceVersion());
        // verify one causedBy
        final CauseLine rootCause = (CauseLine) lines.get(23);
        Assert.assertEquals("java.net.ConnectException", rootCause.getClassName());
        Assert.assertEquals("Connection refused", rootCause.getMessage());
        // verify last bit
        final StackTraceEndLine endLine = (StackTraceEndLine) lines.get(47);
        Assert.assertEquals(27, endLine.getHowManyOmmitted());
    }

    @Test
    public void testExceptionWithCauses2() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<ExceptionLine>(
                ExceptionParser.INSTANCE.parse(ExceptionParserTest.parseIntoLines(this.getClass().getResourceAsStream(
                        "exception-with-causes2.txt"))));
        Assert.assertEquals(62, lines.size()); // one item per line in the file
        // verify random cause
        final CauseLine cause = (CauseLine) lines.get(29);
        Assert.assertEquals("org.hibernate.exception.ConstraintViolationException", cause.getClassName());
        Assert.assertEquals("could not insert: [com.example.myproject.MyEntity]", cause.getMessage());
        // verify random unknown method
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(48);
        Assert.assertEquals(Source.UNKNOWN, stackTraceLine.getSource());
        Assert.assertEquals("sun.reflect.GeneratedMethodAccessor5.invoke", stackTraceLine.getMethodName());
        Assert.assertEquals(null, stackTraceLine.getClassSource());
        Assert.assertEquals(null, stackTraceLine.getClassSourceVersion());
        // verify one stacktrace-ending bit
        final StackTraceEndLine endLine = (StackTraceEndLine) lines.get(28);
        Assert.assertEquals(27, endLine.getHowManyOmmitted());
    }

    @Test
    public void testMavenProducedException() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<ExceptionLine>(
                ExceptionParser.INSTANCE.parse(ExceptionParserTest.parseIntoLines(this.getClass().getResourceAsStream(
                        "exception-maven.txt"))));
        Assert.assertEquals(33, lines.size()); // one item per line in the file
        // verify the initial cause
        final CauseLine firstLine = (CauseLine) lines.get(0);
        Assert.assertEquals("java.lang.IllegalStateException", firstLine.getClassName());
        Assert.assertEquals(
                "There is no context available for qualifier org.jboss.arquillian.drone.api.annotation.Default. Available contexts are [].",
                firstLine.getMessage());
        // verify one random stack trace line
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(14);
        Assert.assertEquals(Source.REGULAR, stackTraceLine.getSource());
        Assert.assertEquals("org.jboss.qa.brms.tools.GrapheneTestListener.onConfigurationFailure",
                stackTraceLine.getMethodName());
        Assert.assertEquals(27, stackTraceLine.getLineInCode());
        Assert.assertEquals("GrapheneTestListener.java", stackTraceLine.getClassName());
        Assert.assertEquals(null, stackTraceLine.getClassSource());
        Assert.assertEquals(null, stackTraceLine.getClassSourceVersion());
    }

}
