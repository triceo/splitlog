package com.github.triceo.splitlog;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;

public abstract class DefaultTailerBaseTest {

    protected static File getTempFile() {
        try {
            return File.createTempFile("splitlog-", ".log");
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temp files.", e);
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
