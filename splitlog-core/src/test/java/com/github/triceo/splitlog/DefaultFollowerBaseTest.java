package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class DefaultFollowerBaseTest extends AbstractSplitlogTest {

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
    protected static void assertProperOrder(final Collection<Message> actualMessages, final Object... expectedMessages) {
        final List<String> actualLines = actualMessages.stream().map(actual -> actual.getLines().get(0)).collect(Collectors.toCollection(LinkedList::new));
        final List<String> expectedLines = new LinkedList<>();
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
    private final LogWatch logwatch;

    public DefaultFollowerBaseTest() {
        this(LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile()));
    }

    public DefaultFollowerBaseTest(final LogWatchBuilder builder) {
        this.builder = builder;
        this.logwatch = builder.build();
    }

    @After
    public synchronized void destroyEverything() {
        DefaultFollowerBaseTest.LOGGER.info("@After started.");
        this.logwatch.stop();
    }

    protected LogWatchBuilder getBuilder() {
        return this.builder;
    }

    protected LogWatch getLogWatch() {
        return this.logwatch;
    }

}
