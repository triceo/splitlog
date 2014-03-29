package com.github.triceo.splitlog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.Message;

/**
 * Has a sole responsibility of starting and stopping {@link Tailer} thread when
 * told so by the {@link DefaultLogWatch}.
 */
final class LogWatchTailingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWatchTailingManager.class);

    private final int bufferSize;
    private final long delayBetweenReads;
    private final long delayedTailerStartInMilliseconds;
    private final ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
    private final AtomicInteger numberOfTimesThatTailerWasStarted = new AtomicInteger(0);
    private final boolean reopenBetweenReads, ignoreExistingContent;
    private ScheduledFuture<?> tailer;
    private final DefaultLogWatch watch;

    public LogWatchTailingManager(final DefaultLogWatch watch, final long delayBetweenReads,
            final long delayForTailerStart, final boolean readFromEnd, final boolean reopenBetweenReads,
            final int bufferSize) {
        this.watch = watch;
        this.delayBetweenReads = delayBetweenReads;
        this.delayedTailerStartInMilliseconds = delayForTailerStart;
        this.bufferSize = bufferSize;
        this.reopenBetweenReads = reopenBetweenReads;
        this.ignoreExistingContent = readFromEnd;
    }

    public boolean isRunning() {
        return this.tailer != null;
    }

    /**
     * Start the tailer on a separate thread. Only when a tailer is running can
     * {@link Follower}s be notified of new {@link Message}s from the log.
     * 
     * @param needsToWait
     *            Whether the start of the tailer needs to be delayed by
     *            {@link #delayedTailerStartInMilliseconds} milliseconds, as
     *            explained in
     *            {@link LogWatchBuilder#getDelayBeforeTailingStarts()}.
     * @return True if the start was scheduled, false if scheduled already.
     */
    public boolean start(final boolean needsToWait) {
        if (this.isRunning()) {
            LogWatchTailingManager.LOGGER.debug("Tailer already running, therefore not starting.");
            return false;
        }
        final Tailer t = new Tailer(this.watch.getWatchedFile(), new LogWatchTailerListener(this.watch),
                this.delayBetweenReads, this.willReadFromEnd(), this.reopenBetweenReads, this.bufferSize);
        final long delay = needsToWait ? this.delayedTailerStartInMilliseconds : 0;
        this.tailer = this.e.schedule(t, delay, TimeUnit.MILLISECONDS);
        if (this.numberOfTimesThatTailerWasStarted.getAndIncrement() == 0) {
            LogWatchTailingManager.LOGGER.debug("Scheduling log tailer for file '{}' with delay of {} milliseconds.",
                    this.watch.getWatchedFile(), delay);
        } else {
            LogWatchTailingManager.LOGGER.debug(
                    "Re-scheduling log tailer for file '{}' with delay of {} milliseconds.",
                    this.watch.getWatchedFile(), delay);
        }
        return true;
    }

    /**
     * Stop the tailer thread, preventing any {@link Follower}s from receiving
     * {@link Message}s.
     * 
     * @return True if stopped, false if never running.
     */
    public boolean stop() {
        if (!this.isRunning()) {
            LogWatchTailingManager.LOGGER.debug("Tailer not running, therefore not terminating.");
            return false;
        }
        // forcibly terminate tailer
        this.tailer.cancel(true);
        this.tailer = null;
        // cancel whatever message processing that was ongoing
        LogWatchTailingManager.LOGGER.debug(
                "Terminated log tailer for file '{}' as the last known Follower has just been terminated.",
                this.watch.getWatchedFile());
        return true;
    }

    /**
     * Block the current thread until the tailer has finished starting. Delayed
     * starts are a possibility with {@link #start(boolean)}.
     */
    public void waitUntilStarted() {
        long remainingDelay = Long.MAX_VALUE;
        // the tailer may have a delayed start; wait until it actually started
        while (remainingDelay > 0) {
            remainingDelay = this.tailer.getDelay(TimeUnit.MILLISECONDS) + 1;
            if (remainingDelay < 0) {
                continue;
            }
            try {
                LogWatchTailingManager.LOGGER.debug("Will wait further {} milliseconds for tailer to be started.",
                        remainingDelay);
                Thread.sleep(remainingDelay);
            } catch (final InterruptedException e) {
                // do nothing
            }
        }
    }

    private boolean willReadFromEnd() {
        if (this.numberOfTimesThatTailerWasStarted.get() > 0) {
            return true;
        } else {
            return this.ignoreExistingContent;
        }
    }

}
