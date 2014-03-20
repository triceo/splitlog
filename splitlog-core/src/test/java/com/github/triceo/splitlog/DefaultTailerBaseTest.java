package com.github.triceo.splitlog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public abstract class DefaultTailerBaseTest {

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
     *            Messages from some {@link LogTailer}.
     * @param expectedMessages
     *            Either {@link Message} to compare messages, or a string if we
     *            want to compare against a tag.
     */
    protected static void assertProperOrder(final List<Message> messages, final Object... expectedMessages) {
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

    public DefaultTailerBaseTest() {
        this(LogWatchBuilder.forFile(DefaultTailerBaseTest.getTempFile()));
    }

    public DefaultTailerBaseTest(final LogWatchBuilder builder) {
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
    }

    @After
    public void destroyEverything() {
        this.writer.destroy();
        if (!this.logwatch.isTerminated()) {
            this.logwatch.terminateTailing();
        }
    }
}
