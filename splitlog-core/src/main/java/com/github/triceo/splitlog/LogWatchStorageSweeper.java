package com.github.triceo.splitlog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.util.SplitlogThreadFactory;

/**
 * Has a sole responsibility of starting and stopping the sweeping of
 * unreachable messages when told so by the {@link DefaultLogWatch}.
 */
final class LogWatchStorageSweeper implements Runnable {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(LogWatchStorageSweeper.class);
    private static final ThreadFactory THREAD_FACTORY = new SplitlogThreadFactory("sweeps");
    private final long delayBetweenSweeps;
    private boolean isStarted = false;
    private boolean isStopped = false;
    private final LogWatchStorageManager messaging;
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1,
            LogWatchStorageSweeper.THREAD_FACTORY);

    public LogWatchStorageSweeper(final LogWatchStorageManager messaging, final LogWatchBuilder builder) {
        this.messaging = messaging;
        this.delayBetweenSweeps = builder.getDelayBetweenSweeps();
    }

    @Override
    public void run() {
        final MessageStore messages = this.messaging.getMessageStore();
        final int minId = this.messaging.getFirstReachableMessageId();
        LogWatchStorageSweeper.LOGGER.debug(
                "Starting message sweep from {}. First reachable message ID reportedly {}.",
                this.messaging.getLogWatch(), minId);
        if (minId < 0) {
            LogWatchStorageSweeper.LOGGER.info("Sweeping all messages from {} as none are reachable.",
                    this.messaging.getLogWatch());
            messages.discardBefore(messages.getNextPosition());
            return;
        } else if (messages.isEmpty()) {
            LogWatchStorageSweeper.LOGGER.info("No messages in {}.", this.messaging.getLogWatch());
            return;
        }
        final int num = messages.discardBefore(minId);
        LogWatchStorageSweeper.LOGGER.info("Swept {} messages from {}.", num, this.messaging.getLogWatch());
    }

    /**
     * Start the sweeping if not started already.
     *
     * @return False if already called before, true otherwise.
     */
    public synchronized boolean start() {
        if (this.isStarted) {
            return false;
        }
        this.isStarted = true;
        final long delay = this.delayBetweenSweeps;
        this.timer.scheduleWithFixedDelay(this, delay, delay, TimeUnit.MILLISECONDS);
        LogWatchStorageSweeper.LOGGER.info(
                "Scheduled automated unreachable message sweep in {} to run every {} millisecond(s).",
                this.messaging.getLogWatch(), delay);
        return true;
    }

    /**
     * Stop the sweeping if not stopped already.
     *
     * @return False if {@link #start()} not called or {@link #stop()} called
     *         already.
     */
    public synchronized boolean stop() {
        if (!this.isStarted || this.isStopped) {
            return false;
        }
        this.isStopped = false;
        this.timer.shutdown();
        LogWatchStorageSweeper.LOGGER.info("Cancelled automated unreachable message sweep in {}.",
                this.messaging.getLogWatch());
        return true;
    }
}
