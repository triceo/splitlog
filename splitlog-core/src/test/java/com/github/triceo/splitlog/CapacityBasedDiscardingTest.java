package com.github.triceo.splitlog;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CapacityBasedDiscardingTest extends DefaultTailerBaseTest {

    // will verify various configs of log watch
    // FIXME share
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()) },
                { LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()).closingAfterReading()
                        .ignoringPreexistingContent() },
                { LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()).closingAfterReading() },
                { LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()).ignoringPreexistingContent() } });
    }

    public CapacityBasedDiscardingTest(final LogWatchBuilder builder) {
        super(builder.limitCapacityTo(1));
    }

    // FIXME share
    private void assertProperOrder(final List<Message> messages, final Object... expectedMessages) {
        Assert.assertEquals(expectedMessages.length, messages.size());
        for (int i = 0; i < expectedMessages.length; i++) {
            final Object expected = expectedMessages[i];
            final Message actual = messages.get(i);
            if (expected instanceof Message) {
                Assert.assertEquals(expected, actual);
            } else {
                Assert.assertEquals(expected, actual.getLines().get(0));
            }
        }
    }

    @Test
    public void test() {
        final LogTailer tail = this.getLogWatch().startTailing();
        // tag before any messages
        final Message firstTag = tail.tag("test");
        this.assertProperOrder(tail.getMessages(), firstTag);
        final String firstMessage = "check";
        this.getWriter().write(firstMessage, tail);
        // receive first message, check presence of tag
        final String secondMessage = "check2";
        this.getWriter().write(secondMessage, tail);
        final Message secondTag = tail.tag("test2");
        this.assertProperOrder(tail.getMessages(), firstTag, firstMessage, secondTag);
        // receive second message, discarding first message and the tag
        final String thirdMessage = "check3";
        this.getWriter().write(thirdMessage, tail);
        this.assertProperOrder(tail.getMessages(), secondTag, secondMessage);
        // receive third message, discarding second message and the tag
        final String fourthMessage = "check4";
        this.getWriter().write(fourthMessage, tail);
        this.assertProperOrder(tail.getMessages(), thirdMessage);
    }

}
