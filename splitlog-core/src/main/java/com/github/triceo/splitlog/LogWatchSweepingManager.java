package com.github.triceo.splitlog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Has a sole responsibility of starting and stopping
 * {@link LogWatchMessageSweeper} thread when told so by the
 * {@link DefaultLogWatch}.
 */
final class LogWatchSweepingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWatchSweepingManager.class);

    private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> currentlyRunningSweeper = null;
    private final long delayBetweenSweeps;
    private final DefaultLogWatch watch;

    public LogWatchSweepingManager(final DefaultLogWatch watch, final long delayBetweenSweeps) {
        this.watch = watch;
        this.delayBetweenSweeps = delayBetweenSweeps;
    }

    public long getDelayBetweenSweeps() {
        return this.delayBetweenSweeps;
    }

    public boolean isRunning() {
        return this.currentlyRunningSweeper != null;
    }

    /**
     * Start the sweeping if not started already.
     * 
     * @return True is started, false if already running.
     */
    public boolean start() {
        if (this.isRunning()) {
            return false;
        }
        final long delay = this.delayBetweenSweeps;
        this.currentlyRunningSweeper = LogWatchSweepingManager.TIMER.scheduleWithFixedDelay(new LogWatchMessageSweeper(
                this.watch), delay, delay, TimeUnit.MILLISECONDS);
        LogWatchSweepingManager.LOGGER
                .debug("Scheduled automated unreachable message sweep in log watch for file '{}' to run every {} millisecond(s).",
                        this.watch.getWatchedFile(), delay);
        return true;
    }

    /**
     * Stop the sweeping if not stopped already.
     * 
     * @return True if stopped, false if stopped already.
     */
    public boolean stop() {
        if (!this.isRunning()) {
            return false;
        }
        this.currentlyRunningSweeper.cancel(false);
        this.currentlyRunningSweeper = null;
        return true;
    }

}
