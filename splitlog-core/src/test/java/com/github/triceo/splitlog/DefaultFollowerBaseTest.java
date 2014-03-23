package com.github.triceo.splitlog;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DefaultFollowerBaseTest {

    private static final String INITIAL_MESSAGE = "INITIAL";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFollowerBaseTest.class);

    // will verify various configs of log watch
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { LogWatchBuilder.forFile(DefaultFollowerBaseTest.getTempFile()) },
                { LogWatchBuilder.forFile(DefaultFollowerBaseTest.getTempFile()).closingAfterReading()
                        .ignoringPreexistingContent() },
                { LogWatchBuilder.forFile(DefaultFollowerBaseTest.getTempFile()).closingAfterReading() },
                { LogWatchBuilder.forFile(DefaultFollowerBaseTest.getTempFile()).ignoringPreexistingContent() } });
    }

    protected static File getTempFile() {
        try {
            return File.createTempFile("splitlog-", ".log");
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temp files.", e);
        }
    }

    /**
     * Will fail unless the messages provided equal to those in the collection,
     * and are present in the exact same order.
     * 
     * @param messages
     *            Messages from some {@link Follower}.
     * @param expectedMessages
     *            Either {@link Message} to compare messages, or a string if we
     *            want to compare against a tag.
     */
    protected static void assertProperOrder(final List<Message> messages, final Object... expectedMessages) {
        System.out.println(messages);
        System.out.println(Arrays.toString(expectedMessages));
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

    private final LogWatchBuilder builder;
    private LogWatch logwatch;
    private LogWriter writer;

    public DefaultFollowerBaseTest() {
        this(LogWatchBuilder.forFile(DefaultFollowerBaseTest.getTempFile()));
    }

    public DefaultFollowerBaseTest(final LogWatchBuilder builder) {
        this.builder = builder;
    }

    protected LogWatchBuilder getBuilder() {
        return this.builder;
    }

    protected LogWatch getLogWatch() {
        return this.logwatch;
    }

    LogWriter getWriter() {
        return this.writer;
    }

    @Before
    public void startEverything() {
        // prepare file
        final File toWrite = this.getBuilder().getFileToWatch();
        if (toWrite.exists()) {
            toWrite.delete();
        }
        try {
            toWrite.createNewFile();
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temp files.", e);
        }
        // prepare writer
        this.writer = new LogWriter(toWrite);
        // and start the log watch
        this.logwatch = this.getBuilder().build();
        // this will write an initial message to the log
        this.writer.writeWithoutWaiting(DefaultFollowerBaseTest.INITIAL_MESSAGE);
        if (!this.getBuilder().isReadingFromBeginning()) {
            return;
        }
        /*
         * if we are reading from beginning, we get rid of the first message;
         * this way, Github issue #25 is tested and all tests still work for
         * both cases.
         */
        DefaultFollowerBaseTest.LOGGER.info("Initial message written.");
        final Follower f = this.getLogWatch().follow();
        try {
            // give the follower some time to be notified of the message
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            DefaultFollowerBaseTest.LOGGER.warn("Test wait failed.");
        }
        this.getLogWatch().unfollow(f);
        DefaultFollowerBaseTest.LOGGER.info("@Before finished.");
    }

    @After
    public void destroyEverything() {
        this.writer.destroy();
        if (!this.logwatch.isTerminated()) {
            this.logwatch.terminate();
        }
    }
}
