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
public class ReferenceBasedDiscardingTest extends DefaultTailerBaseTest {

    // will verify various configs of log watch
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()) },
                { LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()).closingAfterReading()
                        .ignoringPreexistingContent() },
                { LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()).closingAfterReading() },
                { LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()).ignoringPreexistingContent() } });
    }

    public ReferenceBasedDiscardingTest(final LogWatchBuilder builder) {
        super(builder);
    }

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
        final DefaultLogWatch w = (DefaultLogWatch) this.getLogWatch();
        LogTailer tail = w.startTailing();
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
        // start second tailer; this one will only track second+ messages
        LogTailer tail2 = w.startTailing();
        // send third message, receive second
        final String thirdMessage = "check3";
        this.getWriter().write(thirdMessage, tail2);
        Assert.assertEquals(2, w.countMessagesInStorage());
        /*
         * remove all references to the first tailer; the first message now has
         * no tailers available and can be GC'd
         */
        w.terminateTailing(tail);
        tail = null;
        System.gc();
        final long delay = w.getDelayBetweenSweeps() + 500;
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
            Assert.fail("Test will fail as there was not enough time to wait for message sweep.");
        }
        Assert.assertEquals(1, w.countMessagesInStorage());
        /*
         * make sure the second tailer has what it's supposed to; the second
         * message
         */
        this.assertProperOrder(tail2.getMessages(), secondMessage);
        // terminate tailing, make sure all the messages are cleared
        w.terminateTailing(tail2);
        tail2 = null;
        System.gc();
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
            Assert.fail("Test will fail as there was not enough time to wait for message sweep.");
        }
        Assert.assertEquals(0, w.countMessagesInStorage());
    }

}
