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
            Assert.assertNull(msg); // make sure the line is not yet processed
            final Message msg2 = splitter.addLine(line + "2");
            /*
             * now the second message has been registered, the original one is
             * processed
             */
            Assert.assertEquals(1, msg2.getLines().size());
            Assert.assertEquals(line, msg2.getLines().get(0));
            Assert.assertNotNull(splitter.forceProcessing());
        }
    }

}
