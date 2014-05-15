package com.github.triceo.splitlog.util;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

/**
 * Extension of the Apache tailer to allow us to monitor when it's actually
 * started tailing.
 */
public class SplitlogTailer extends Tailer {

    private final CountDownLatch started = new CountDownLatch(1);

    public SplitlogTailer(final File file, final TailerListener listener, final long delayMillis, final boolean end,
        final boolean reOpen, final int bufSize) {
        super(file, listener, delayMillis, end, reOpen, bufSize);
    }

    @Override
    public void run() {
        this.started.countDown();
        super.run();
    }

    public void waitUntilStarted() {
        try {
            this.started.await();
        } catch (final InterruptedException e) {
            this.waitUntilStarted();
        }
    }

}
