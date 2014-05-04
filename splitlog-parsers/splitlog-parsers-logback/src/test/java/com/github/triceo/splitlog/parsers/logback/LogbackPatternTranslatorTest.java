package com.github.triceo.splitlog.parsers.logback;

import java.util.Collection;
import java.util.LinkedList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.triceo.splitlog.parsers.SplitlogPatternParser;
import com.github.triceo.splitlog.parsers.logback.LogbackPatternTranslator;

@RunWith(Parameterized.class)
public class LogbackPatternTranslatorTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> getPatterns() {
        Collection<Object[]> patterns = new LinkedList<Object[]>();
        patterns.add(new String[] {"%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"});
        patterns.add(new String[] {"%date %level [%thread] %logger{10} [%file:%line] %msg%n"});
        patterns.add(new String[] {"%-5level [%thread]: %message%n"});
        patterns.add(new String[] {"%d %contextName [%t] %level %logger{36} - %msg%n"});
        patterns.add(new String[] {"%-4relative [%thread] %-5level - %msg%n%caller{2, DISP_CALLER_EVAL, OTHER_EVAL_NAME, THIRD_EVAL_NAME}"});
        patterns.add(new String[] {"%-5level - %replace(%msg){'\\d{14,16}', 'XXXX'}%n"});
        patterns.add(new String[] {"%-30(%d{HH:mm:ss.SSS} [%thread]) %-5level %logger{32} - %msg%n"});
        patterns.add(new String[] {"[%thread] %highlight(%-5level) %cyan(%logger{15}) - [%msg %n]"});
        patterns.add(new String[] {"%-4relative [%thread] %-5level - %msg%n%caller{2, DISP_CALLER_EVAL}"});
        patterns.add(new String[] {"%d ${CONTEXT_NAME} %level %msg %logger{50}%n"});
        return patterns;
    }
    
    private final String pattern;
        
    public LogbackPatternTranslatorTest(String pattern) {
        this.pattern = pattern;
    }
    
    @Test
    public void test() {
        SplitlogPatternParser.build(new LogbackPatternTranslator(), pattern);
    }

}
