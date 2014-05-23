package com.github.triceo.splitlog.util;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    public SplitlogTailer(final File file, final TailerListener listener, final long delayMillis, final boolean end,
        final boolean reOpen, final int bufSize) {
        super(file, listener, delayMillis, end, reOpen, bufSize);
    }

    @Override
    public void run() {
        SplitlogTailer.LOGGER.info("Tailer thread started.");
        this.started.countDown();
        final long start = System.nanoTime();
        super.run();
        final long result = System.nanoTime() - start;
        SplitlogTailer.LOGGER.info("Tailer thread stopped, took {} ms.", TimeUnit.NANOSECONDS.toMillis(result));
    }

    public void waitUntilStarted() {
        try {
            this.started.await();
        } catch (final InterruptedException e) {
            this.waitUntilStarted();
        }
    }

}
