package com.github.triceo.splitlog.util;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;

import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

/**
 * Extension of the Apache tailer to allow us to monitor when it's actually
 * started tailing.
 */
public class SplitlogTailer extends Tailer {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(SplitlogTailer.class);

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch stopped = new CountDownLatch(1);

    public SplitlogTailer(final File file, final TailerListener listener, final long delayMillis, final boolean end,
        final boolean reOpen, final int bufSize) {
        super(file, listener, delayMillis, end, reOpen, bufSize);
    }

    @Override
    public void run() {
        try {
            SplitlogTailer.LOGGER.info("Tailer thread started.");
            this.started.countDown();
            SplitlogTailer.LOGGER.debug("Tailer super.run() starting.");
            super.run();
            SplitlogTailer.LOGGER.debug("Tailer super.run() finished.");
        } finally {
            this.stopped.countDown();
            SplitlogTailer.LOGGER.info("Tailer thread stopped.");
        }
    }

    public void waitUntilStarted() {
        try {
            this.started.await();
        } catch (final InterruptedException e) {
            this.waitUntilStarted();
        }
    }

    public void waitUntilStopped() {
        try {
            this.stopped.await();
        } catch (final InterruptedException e) {
            this.waitUntilStopped();
        }
    }
}
