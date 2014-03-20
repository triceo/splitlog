package com.github.triceo.splitlog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CapacityBasedDiscardingTest extends DefaultTailerBaseTest {

    public CapacityBasedDiscardingTest(final LogWatchBuilder builder) {
        super(builder.limitCapacityTo(1));
    }

    // FIXME share

    @Test
    public void test() {
        final LogTailer tail = this.getLogWatch().startTailing();
        // tag before any messages
        final Message firstTag = tail.tag("test");
        DefaultTailerBaseTest.assertProperOrder(tail.getMessages(), firstTag);
        final String firstMessage = "check";
        this.getWriter().write(firstMessage, tail);
        // receive first message, check presence of tag
        final String secondMessage = "check2";
        this.getWriter().write(secondMessage, tail);
        final Message secondTag = tail.tag("test2");
        DefaultTailerBaseTest.assertProperOrder(tail.getMessages(), firstTag, firstMessage, secondTag);
        // receive second message, discarding first message and the tag
        final String thirdMessage = "check3";
        this.getWriter().write(thirdMessage, tail);
        DefaultTailerBaseTest.assertProperOrder(tail.getMessages(), secondTag, secondMessage);
        // receive third message, discarding second message and the tag
        final String fourthMessage = "check4";
        this.getWriter().write(fourthMessage, tail);
        DefaultTailerBaseTest.assertProperOrder(tail.getMessages(), thirdMessage);
    }

}
