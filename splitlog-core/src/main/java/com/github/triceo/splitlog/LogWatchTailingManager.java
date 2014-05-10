package com.github.triceo.splitlog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

/**
 * Has a sole responsibility of starting and stopping {@link Tailer} thread when
 * told so by the {@link DefaultLogWatch}.
 */
final class LogWatchTailingManager {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(LogWatchTailingManager.class);

    private final int bufferSize;
    private final long delayBetweenReads;
    private final ExecutorService e = Executors.newFixedThreadPool(1, new ThreadFactory() {

        private final AtomicLong ID_GENERATOR = new AtomicLong(0);

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "tailing-" + this.ID_GENERATOR.incrementAndGet());
        }

    });
    private final AtomicInteger numberOfTimesThatTailerWasStarted = new AtomicInteger(0);
    private final boolean reopenBetweenReads, ignoreExistingContent;
    private Future<?> tailer;
    private final DefaultLogWatch watch;

    public LogWatchTailingManager(final DefaultLogWatch watch, final long delayBetweenReads, final boolean readFromEnd,
            final boolean reopenBetweenReads, final int bufferSize) {
        this.watch = watch;
        this.delayBetweenReads = delayBetweenReads;
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
     * @return True if the start was scheduled, false if scheduled already.
     */
    public boolean start() {
        if (this.isRunning()) {
            return false;
        }
        final Tailer t = new Tailer(this.watch.getWatchedFile(), new LogWatchTailerListener(this.watch),
                this.delayBetweenReads, this.willReadFromEnd(), this.reopenBetweenReads, this.bufferSize);
        this.tailer = this.e.submit(t);
        LogWatchTailingManager.LOGGER.info("Started tailer for {}.", this.watch);
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
        LogWatchTailingManager.LOGGER.info(
                "Terminated log tailer for {} as the last known Follower has just been terminated.", this.watch);
        return true;
    }

    private boolean willReadFromEnd() {
        if (this.numberOfTimesThatTailerWasStarted.get() > 0) {
            return true;
        } else {
            return this.ignoreExistingContent;
        }
    }

}
