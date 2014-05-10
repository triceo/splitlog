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
     * @param messages
     *            Messages from some {@link Follower}.
     * @param expectedMessages
     *            Either {@link Message} to compare messages, or a string if we
     *            want to compare against a tag.
     */
    protected static void assertProperOrder(final SortedSet<Message> messages, final Object... expectedMessages) {
        Assertions.assertThat(messages.size()).isEqualTo(expectedMessages.length);
        final List<Message> indexableMessages = new LinkedList<Message>(messages);
        for (int i = 0; i < expectedMessages.length; i++) {
            final Object expected = expectedMessages[i];
            final Message actual = indexableMessages.get(i);
            if (expected instanceof Message) {
                Assertions.assertThat(actual).isEqualTo(expected);
            } else {
                Assertions.assertThat(actual.getLines().get(0)).isEqualTo(expected);
            }
        }
    }

    // will verify various configs of log watch
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { LogWatchBuilder.getDefault().watchingFile(DefaultFollowerBaseTest.getTempFile()) },
                { LogWatchBuilder.getDefault().watchingFile(DefaultFollowerBaseTest.getTempFile())
                    .closingAfterReading()
                    .ignoringPreexistingContent() },
                    { LogWatchBuilder.getDefault().watchingFile(DefaultFollowerBaseTest.getTempFile())
                        .closingAfterReading() },
                        { LogWatchBuilder.getDefault().watchingFile(DefaultFollowerBaseTest.getTempFile())
                            .ignoringPreexistingContent() } });
    }

    protected static File getTempFile() {
        try {
            return File.createTempFile("splitlog-", ".log");
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temp files.", e);
        }
    }

    public static Message wrapWaiting(final Future<Message> message) {
        return DefaultFollowerBaseTest.wrapWaiting(message, 1, TimeUnit.MINUTES);
    }

    public static Message wrapWaiting(final Future<Message> message, final long timeout, final TimeUnit unit) {
        try {
            return message.get(timeout, unit);
        } catch (final Exception e) {
            return null;
        }
    }

    private final LogWatchBuilder builder;
    private LogWatch logwatch;
    private LogWriter writer;

    public DefaultFollowerBaseTest() {
        this(LogWatchBuilder.getDefault().watchingFile(DefaultFollowerBaseTest.getTempFile()));
    }

    public DefaultFollowerBaseTest(final LogWatchBuilder builder) {
        this.builder = builder;
    }

    @After
    public void destroyEverything() {
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
        if (!this.getBuilder().isReadingFromBeginning()) {
            return;
        }
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
        DefaultFollowerBaseTest.LOGGER.info("@Before finished.");
    }
}
