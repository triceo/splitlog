package com.github.triceo.splitlog;

import org.junit.Assert;
import org.junit.Test;

public class SimpleTailSplitterTest {

    @Test
    public void test() {
        final String line = "Test";
        final SimpleTailSplitter splitter = new SimpleTailSplitter();
        for (int i = 0; i < 10; i++) {
            // send a couple lines through the splitter
            final Message msg = splitter.addLine(line);
            Assert.assertNotNull(msg); // make sure the line is processed
            Assert.assertEquals(1, msg.getLines().size());
            Assert.assertEquals(line, msg.getLines().get(0));
        }
        Assert.assertNull(splitter.forceProcessing());
    }

}
