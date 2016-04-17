package com.github.triceo.splitlog.splitters.exceptions;

import com.github.triceo.splitlog.AbstractSplitlogTest;
import com.github.triceo.splitlog.splitters.exceptions.StackTraceLine.Source;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ExceptionParserTest extends AbstractSplitlogTest {

    // FIXME share this in a better way
    public static Collection<String> parseIntoLines(final InputStream s) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(s));
            final Collection<String> lines = new ArrayList<>();
            String line;
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
    public void testExceptionWhoseFirstLineDoesntBeginWithClassName() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<>(new ExceptionParser().parse(ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("exception-with-line-prefix.txt"))));
        // one item per line in the file
        Assertions.assertThat(lines.size()).isEqualTo(31);
        // verify the initial cause
        final CauseLine firstLine = (CauseLine) lines.get(0);
        Assertions.assertThat(firstLine.getClassName()).isEqualTo("org.switchyard.SwitchYardException");
        Assertions.assertThat(firstLine.getMessage()).isEqualTo(
                "SWITCHYARD014032: Operation fail does not exist for service {urn:ledegen:operation-selector-service:1.0}SimpleHttpGreetingGateway");
        // verify one random stack trace line
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(5);
        Assertions.assertThat(stackTraceLine.getSource()).isEqualTo(Source.REGULAR);
        Assertions.assertThat(stackTraceLine.getMethodName()).isEqualTo("javax.servlet.http.HttpServlet.service");
        Assertions.assertThat(stackTraceLine.getLineInCode()).isEqualTo(847);
        Assertions.assertThat(stackTraceLine.getClassName()).isEqualTo("HttpServlet.java");
        Assertions.assertThat(stackTraceLine.getClassSource()).isEqualTo(
                "jboss-servlet-api_3.0_spec-1.0.2.Final-redhat-1.jar");
        Assertions.assertThat(stackTraceLine.getClassSourceVersion()).isEqualTo("1.0.2.Final-redhat-1");
        // verify last line
        final StackTraceLine stackTraceLine2 = (StackTraceLine) lines.get(30);
        Assertions.assertThat(stackTraceLine2.getSource()).isEqualTo(Source.REGULAR);
        Assertions.assertThat(stackTraceLine2.getMethodName()).isEqualTo("java.lang.Thread.run");
        Assertions.assertThat(stackTraceLine2.getLineInCode()).isEqualTo(744);
        Assertions.assertThat(stackTraceLine2.getClassName()).isEqualTo("Thread.java");
        Assertions.assertThat(stackTraceLine2.getClassSource()).isEqualTo("rt.jar");
        Assertions.assertThat(stackTraceLine2.getClassSourceVersion()).isEqualTo("1.7.0_51");
    }

    @Test
    public void testExceptionWithCauses() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<>(new ExceptionParser().parse(ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("exception-with-causes.txt"))));
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
        final List<ExceptionLine> lines = new ArrayList<>(new ExceptionParser().parse(ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("exception-with-causes2.txt"))));
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
        final List<ExceptionLine> lines = new ArrayList<>(new ExceptionParser().parse(ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("exception-maven.txt"))));
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

    @Test
    public void testIssue57B() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<>(new ExceptionParser().parse(ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("issue-57_2.txt"))));
        Assertions.assertThat(lines.size()).isEqualTo(18); // one item per line
        // in the file
        // verify the initial cause
        final CauseLine firstLine = (CauseLine) lines.get(0);
        Assertions.assertThat(firstLine.getClassName()).isEqualTo("org.switchyard.SwitchYardException");
        Assertions
                .assertThat(firstLine.getMessage())
                .isEqualTo(
                        "SWITCHYARD014032: Operation fail does not exist for service {urn:ledegen:operation-selector-service:1.0}SimpleHttpGreetingService");
        // verify one random stack trace line
        // 	at java.lang.Thread.run(Thread.java:745) [rt.jar:1.8.0_45]
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(17);
        Assertions.assertThat(stackTraceLine.getSource()).isEqualTo(Source.REGULAR);
        Assertions.assertThat(stackTraceLine.getMethodName()).isEqualTo(
                "java.lang.Thread.run");
        Assertions.assertThat(stackTraceLine.getLineInCode()).isEqualTo(745);
        Assertions.assertThat(stackTraceLine.getClassName()).isEqualTo("Thread.java");
        Assertions.assertThat(stackTraceLine.getClassSource()).isEqualTo("rt.jar");
        Assertions.assertThat(stackTraceLine.getClassSourceVersion()).isEqualTo("1.8.0_45");
    }

    @Test
    public void testIssue57A() throws ExceptionParseException {
        final List<ExceptionLine> lines = new ArrayList<>(new ExceptionParser().parse(ExceptionParserTest
                .parseIntoLines(this.getClass().getResourceAsStream("issue-57_1.txt"))));
        Assertions.assertThat(lines.size()).isEqualTo(23); // one item per line
        // in the file
        // verify the initial cause
        final CauseLine firstLine = (CauseLine) lines.get(0);
        Assertions.assertThat(firstLine.getClassName()).isEqualTo("org.switchyard.SwitchYardException");
        // verify one random stack trace line
        // 	at java.lang.Thread.run(Thread.java:745) [rt.jar:1.8.0_45]
        final StackTraceLine stackTraceLine = (StackTraceLine) lines.get(22);
        Assertions.assertThat(stackTraceLine.getSource()).isEqualTo(Source.REGULAR);
        Assertions.assertThat(stackTraceLine.getMethodName()).isEqualTo(
                "java.lang.Thread.run");
        Assertions.assertThat(stackTraceLine.getLineInCode()).isEqualTo(745);
        Assertions.assertThat(stackTraceLine.getClassName()).isEqualTo("Thread.java");
        Assertions.assertThat(stackTraceLine.getClassSource()).isEqualTo("rt.jar");
        Assertions.assertThat(stackTraceLine.getClassSourceVersion()).isEqualTo("1.8.0_45");
    }

}
