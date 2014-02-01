package com.github.triceo.splitlog;

import org.junit.Assert;
import org.junit.Test;

public class PerLineTailSplitterTest {

    @Test
    public void test() {
        final String line = "Test";
        final PerLineTailSplitter splitter = new PerLineTailSplitter();
        for (int i = 0; i < 10; i++) { // send a couple lines through the
                                       // splitter
            final RawMessage msg = splitter.addLine(line);
            Assert.assertNotNull(msg); // make sure the line is processed
            Assert.assertEquals(1, msg.getLines().size()); // make sure there is
                                                           // only one line
            Assert.assertEquals(line, msg.getFirstLine()); // and that it is the
                                                           // expected one
        }
        Assert.assertNull(splitter.forceProcessing()); // and ensure that there
                                                       // is nothing in the
                                                       // buffer
    }

}
