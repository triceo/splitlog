package com.github.triceo.splitlog.splitters;

import org.junit.Assert;
import org.junit.Test;

import com.github.triceo.splitlog.Message;

public class JBossServerLogTailSplitterTest {

    private static final String STDERR_META = "(http-/127.0.0.1:8080-20) java.lang.IllegalArgumentException: A RuleFlowProcess cannot have more than one start node!";
    private static final String STDERR = "12:16:32,756 ERROR [stderr] " + JBossServerLogTailSplitterTest.STDERR_META;
    private static final String LOG_META = "[org.jbpm.ruleflow.core.validation.RuleFlowProcessValidator] (http-/127.0.0.1:8080-20) Process variable data uses ObjectDataType for default type (java.lang) which could cause problems with setting variables, use dedicated type instead";
    private static final String LOG = "12:16:32,500 WARN  " + JBossServerLogTailSplitterTest.LOG_META;
    private static final String LOG2 = "Continuation";

    @Test
    public void testLog() {
        final TailSplitter splitter = new JBossServerLogTailSplitter();
        // send a couple lines through the splitter
        final Message msg = splitter.addLine(JBossServerLogTailSplitterTest.LOG);
        Assert.assertNull(msg); // make sure the line is not yet processed
        final Message msg2 = splitter.addLine(JBossServerLogTailSplitterTest.LOG2);
        Assert.assertNull(msg2); // make sure the lines are still not processed
        final Message msg3 = splitter.addLine(JBossServerLogTailSplitterTest.STDERR);
        Assert.assertNotNull(msg3); // we should get results of lines 1 and 2
        Assert.assertEquals(2, msg3.getLines().size());
        Assert.assertEquals(JBossServerLogTailSplitterTest.LOG_META.trim(), msg3.getLines().get(0));
        // one line is remaining to process
        Assert.assertNotNull(splitter.forceProcessing());
    }

    @Test
    public void testStderr() {
        final TailSplitter splitter = new JBossServerLogTailSplitter();
        // send a couple lines through the splitter
        final Message msg = splitter.addLine(JBossServerLogTailSplitterTest.STDERR);
        Assert.assertNull(msg); // make sure the line is not yet processed
        final Message msg2 = splitter.addLine(JBossServerLogTailSplitterTest.LOG);
        Assert.assertNotNull(msg2); // we should get results of first message
        Assert.assertEquals(1, msg2.getLines().size());
        Assert.assertEquals(JBossServerLogTailSplitterTest.STDERR_META.trim(), msg2.getLines().get(0));
        // one line is remaining to process
        Assert.assertNotNull(splitter.forceProcessing());
    }
}
