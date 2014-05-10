package com.github.triceo.splitlog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {

        private final ThreadGroup group = new ThreadGroup("tailing");
        private final AtomicLong nextId = new AtomicLong(0);

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(this.group, r, this.group.getName() + "-" + this.nextId.incrementAndGet());
        }

    });

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(LogWatchTailingManager.class);
    private final int bufferSize;
    private final long delayBetweenReads;
    private final AtomicLong numberOfTimesThatTailerWasStarted = new AtomicLong(0);
    private final boolean reopenBetweenReads, ignoreExistingContent;
    private Tailer tailer;
    private final DefaultLogWatch watch;

    public LogWatchTailingManager(final DefaultLogWatch watch, final long delayBetweenReads, final boolean readFromEnd,
        final boolean reopenBetweenReads, final int bufferSize) {
        this.watch = watch;
        this.delayBetweenReads = delayBetweenReads;
        this.bufferSize = bufferSize;
        this.reopenBetweenReads = reopenBetweenReads;
        this.ignoreExistingContent = readFromEnd;
    }

    public synchronized boolean isRunning() {
        return (this.tailer != null);
    }

    /**
     * Start the tailer on a separate thread. Only when a tailer is running can
     * {@link Follower}s be notified of new {@link Message}s from the log.
     *
     * @return True if the start was scheduled, false if scheduled already.
     */
    public synchronized boolean start() {
        if (this.isRunning()) {
            return false;
        }
        this.tailer = new Tailer(this.watch.getWatchedFile(), new LogWatchTailerListener(this.watch),
                this.delayBetweenReads, this.willReadFromEnd(), this.reopenBetweenReads, this.bufferSize);
        LogWatchTailingManager.EXECUTOR.submit(this.tailer);
        final long iterationNum = this.numberOfTimesThatTailerWasStarted.incrementAndGet();
        LogWatchTailingManager.LOGGER.info("Tailing #{} started for {}.", iterationNum, this.watch);
        return true;
    }

    /**
     * Stop the tailer thread, preventing any {@link Follower}s from receiving
     * {@link Message}s.
     *
     * @return True if stopped, false if never running.
     */
    public synchronized boolean stop() {
        if (!this.isRunning()) {
            LogWatchTailingManager.LOGGER.debug("Tailer not running, therefore not terminating.");
            return false;
        }
        // forcibly terminate tailer
        this.tailer.stop();
        this.tailer = null;
        LogWatchTailingManager.LOGGER.info("Terminated tailing #{} for {}.",
                this.numberOfTimesThatTailerWasStarted.get(), this.watch);
        return true;
    }

    private synchronized boolean willReadFromEnd() {
        if (this.numberOfTimesThatTailerWasStarted.get() > 0) {
            return true;
        } else {
            return this.ignoreExistingContent;
        }
    }

}
