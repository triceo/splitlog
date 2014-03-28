package com.github.triceo.splitlog.splitters.exceptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.splitters.exceptions.CauseLine;
import com.github.triceo.splitlog.splitters.exceptions.ExceptionLine;
import com.github.triceo.splitlog.splitters.exceptions.ExceptionParseException;
import com.github.triceo.splitlog.splitters.exceptions.ExceptionParser;
import com.github.triceo.splitlog.splitters.exceptions.StackTraceEndLine;
import com.github.triceo.splitlog.splitters.exceptions.StackTraceLine;
import com.github.triceo.splitlog.splitters.exceptions.StackTraceLine.Source;

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
        Assertions.assertThat(lines.size()).isEqualTo(48); // one item per line
                                                           // in the file
        // verify the initial cause
        final CauseLine firstLine = (CauseLine) lines.get(0);
        Assertions.assertThat(firstLine.getClassName()).isEqualTo("org.jboss.qa.bpms.rest.wb.RequestFailureException");
        Assertions.assertThat(firstLine.getMessage()).isEqualTo("java.net.ConnectException: Connection refused");
        // verify one native method
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(7);
        Assertions.assertThat(stackTraceLine.getSource()).isEqualTo(Source.NATIVE);
        Assertions.assertThat(stackTraceLine.getMethodName()).isEqualTo("sun.reflect.NativeMethodAccessorImpl.invoke0");
        Assertions.assertThat(stackTraceLine.getClassSource()).isEqualTo("na");
        Assertions.assertThat(stackTraceLine.getClassSourceVersion()).isEqualTo("1.7.0_51");
        // verify one causedBy
        final CauseLine rootCause = (CauseLine) lines.get(23);
        Assertions.assertThat(rootCause.getClassName()).isEqualTo("java.net.ConnectException");
        Assertions.assertThat(rootCause.getMessage()).isEqualTo("Connection refused");
        // verify last bit
        final StackTraceEndLine endLine = (StackTraceEndLine) lines.get(47);
        Assertions.assertThat(endLine.getHowManyOmmitted()).isEqualTo(27);
    }

    @Test
    public void testExceptionWithCauses2() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<ExceptionLine>(
                ExceptionParser.INSTANCE.parse(ExceptionParserTest.parseIntoLines(this.getClass().getResourceAsStream(
                        "exception-with-causes2.txt"))));
        Assertions.assertThat(lines.size()).isEqualTo(62); // one item per line
                                                           // in the file
        // verify random cause
        final CauseLine cause = (CauseLine) lines.get(29);
        Assertions.assertThat(cause.getClassName()).isEqualTo("org.hibernate.exception.ConstraintViolationException");
        Assertions.assertThat(cause.getMessage()).isEqualTo("could not insert: [com.example.myproject.MyEntity]");
        // verify random unknown method
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(48);
        Assertions.assertThat(stackTraceLine.getSource()).isEqualTo(Source.UNKNOWN);
        Assertions.assertThat(stackTraceLine.getMethodName()).isEqualTo("sun.reflect.GeneratedMethodAccessor5.invoke");
        Assertions.assertThat(stackTraceLine.getClassSource()).isEqualTo(null);
        Assertions.assertThat(stackTraceLine.getClassSourceVersion()).isEqualTo(null);
        // verify one stacktrace-ending bit
        final StackTraceEndLine endLine = (StackTraceEndLine) lines.get(28);
        Assertions.assertThat(endLine.getHowManyOmmitted()).isEqualTo(27);
    }

    @Test
    public void testMavenProducedException() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<ExceptionLine>(
                ExceptionParser.INSTANCE.parse(ExceptionParserTest.parseIntoLines(this.getClass().getResourceAsStream(
                        "exception-maven.txt"))));
        Assertions.assertThat(lines.size()).isEqualTo(33); // one item per line
                                                           // in the file
        // verify the initial cause
        final CauseLine firstLine = (CauseLine) lines.get(0);
        Assertions.assertThat(firstLine.getClassName()).isEqualTo("java.lang.IllegalStateException");
        Assertions
                .assertThat(firstLine.getMessage())
                .isEqualTo(
                        "There is no context available for qualifier org.jboss.arquillian.drone.api.annotation.Default. Available contexts are [].");
        // verify one random stack trace line
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(14);
        Assertions.assertThat(stackTraceLine.getSource()).isEqualTo(Source.REGULAR);
        Assertions.assertThat(stackTraceLine.getMethodName()).isEqualTo(
                "org.jboss.qa.brms.tools.GrapheneTestListener.onConfigurationFailure");
        Assertions.assertThat(stackTraceLine.getLineInCode()).isEqualTo(27);
        Assertions.assertThat(stackTraceLine.getClassName()).isEqualTo("GrapheneTestListener.java");
        Assertions.assertThat(stackTraceLine.getClassSource()).isEqualTo(null);
        Assertions.assertThat(stackTraceLine.getClassSourceVersion()).isEqualTo(null);
    }

}
