package com.github.triceo.splitlog;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;

public abstract class DefaultFollowerBaseTest extends AbstractSplitlogTest {

    private static final String INITIAL_MESSAGE = "INITIAL";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFollowerBaseTest.class);

    /**
     * Will fail unless the messages provided equal to those in the collection,
     * and are present in the exact same order.
     *
     * @param actualMessages
     *            Messages from some {@link Follower}.
     * @param expectedMessages
     *            Either {@link Message} to compare messages, or a string if we
     *            want to compare against a tag.
     */
    protected static void assertProperOrder(final SortedSet<Message> actualMessages, final Object... expectedMessages) {
        final List<String> actualLines = new LinkedList<String>();
        for (final Message actual : actualMessages) {
            actualLines.add(actual.getLines().get(0));
        }
        final List<String> expectedLines = new LinkedList<String>();
        for (final Object expected : expectedMessages) {
            if (expected instanceof Message) {
                expectedLines.add(((Message) expected).getLines().get(0));
            } else if (expected instanceof String) {
                expectedLines.add((String) expected);
            } else {
                Assertions.fail("Unexpected message: " + expected);
            }
        }
        Assertions.assertThat(actualLines).containsExactly(expectedLines.toArray(new String[expectedLines.size()]));
    }

    // will verify various configs of log watch
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays
                .asList(new Object[][] {
                        { LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile()) },
                        { LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile()).closingAfterReading()
                            .ignoringPreexistingContent() },
                            { LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile()).closingAfterReading() },
                            { LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile())
                                .ignoringPreexistingContent() } });
    }

    public static Message wrapWaiting(final Future<Message> message) {
        return DefaultFollowerBaseTest.wrapWaiting(message, 1, TimeUnit.MINUTES);
    }

    public static Message wrapWaiting(final Future<Message> message, final long timeout, final TimeUnit unit) {
        try {
            return message.get(timeout, unit);
        } catch (final Exception e) {
            Assertions.fail("Waiting for message failed.", e);
            return null;
        }
    }

    private final LogWatchBuilder builder;
    private LogWatch logwatch;
    private LogWriter writer;

    public DefaultFollowerBaseTest() {
        this(LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile()));
    }

    public DefaultFollowerBaseTest(final LogWatchBuilder builder) {
        this.builder = builder;
    }

    @After
    public void destroyEverything() {
        DefaultFollowerBaseTest.LOGGER.info("@After started.");
        this.writer.dispose();
        if (!this.logwatch.isTerminated()) {
            this.logwatch.terminate();
        }
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
        this.writer = LogWriter.forFile(toWrite);
        // and start the log watch
        this.logwatch = this.getBuilder().build();
        // this will write an initial message to the log
        this.writer.writeNow(DefaultFollowerBaseTest.INITIAL_MESSAGE);
        if (this.getBuilder().isReadingFromBeginning()) {
            /*
             * if we are reading from beginning, we get rid of the first message;
             * this way, Github issue #25 is tested and all tests still work for
             * both cases.
             */
            DefaultFollowerBaseTest.LOGGER.info("Initial message written.");
            final Follower f = this.getLogWatch().startFollowing();
            try {
                // give the follower some time to be notified of the message
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                DefaultFollowerBaseTest.LOGGER.warn("Test wait failed.");
            }
            this.getLogWatch().stopFollowing(f);
        }
        DefaultFollowerBaseTest.LOGGER.info("@Before finished.");
    }
}
