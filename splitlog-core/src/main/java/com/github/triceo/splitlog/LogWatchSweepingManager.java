package com.github.triceo.splitlog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.util.SplitlogThreadFactory;

/**
 * Has a sole responsibility of starting and stopping
 * {@link LogWatchStorageSweeper} thread when told so by the
 * {@link DefaultLogWatch}.
 */
final class LogWatchSweepingManager {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(LogWatchSweepingManager.class);
    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1,
            new SplitlogThreadFactory("sweeps"));
    private ScheduledFuture<?> currentlyRunningSweeper = null;
    private final long delayBetweenSweeps;
    private final LogWatchStorageManager messaging;

    public LogWatchSweepingManager(final LogWatchStorageManager messaging, final long delayBetweenSweeps) {
        this.messaging = messaging;
        this.delayBetweenSweeps = delayBetweenSweeps;
    }

    public synchronized boolean isRunning() {
        return this.currentlyRunningSweeper != null;
    }

    /**
     * Start the sweeping if not started already.
     *
     * @return True is started, false if already running.
     */
    public synchronized boolean start() {
        if (this.isRunning()) {
            return false;
        }
        final long delay = this.delayBetweenSweeps;
        this.currentlyRunningSweeper = LogWatchSweepingManager.TIMER.scheduleWithFixedDelay(new LogWatchStorageSweeper(
                this.messaging), delay, delay, TimeUnit.MILLISECONDS);
        LogWatchSweepingManager.LOGGER.info(
                "Scheduled automated unreachable message sweep in {} to run every {} millisecond(s).",
                this.messaging.getLogWatch(), delay);
        return true;
    }

    /**
     * Stop the sweeping if not stopped already.
     *
     * @return True if stopped, false if stopped already.
     */
    public synchronized boolean stop() {
        if (!this.isRunning()) {
            return false;
        }
        this.currentlyRunningSweeper.cancel(false);
        this.currentlyRunningSweeper = null;
        LogWatchSweepingManager.LOGGER.info("Cancelled automated unreachable message sweep in {}.",
                this.messaging.getLogWatch());
        return true;
    }

}
