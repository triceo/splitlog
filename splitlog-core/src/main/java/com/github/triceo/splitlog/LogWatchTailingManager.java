package com.github.triceo.splitlog;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.TailSplitter;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.util.SplitlogTailer;
import com.github.triceo.splitlog.util.SplitlogThreadFactory;

/**
 * Has a sole responsibility of starting and stopping {@link Tailer} thread when
 * told so by the {@link DefaultLogWatch}.
 */
final class LogWatchTailingManager {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new SplitlogThreadFactory("tails"));

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(LogWatchTailingManager.class);
    private final int bufferSize;
    private MessageBuilder currentlyProcessedMessage;
    private final long delayBetweenReads;
    private final AtomicBoolean isReading = new AtomicBoolean(false);
    private final AtomicLong numberOfTimesThatTailerWasStarted = new AtomicLong(0);
    private WeakReference<Message> previousAcceptedMessage;
    private final boolean reopenBetweenReads, ignoreExistingContent;
    private final TailSplitter splitter;
    private SplitlogTailer tailer;

    private final DefaultLogWatch watch;

    public LogWatchTailingManager(final DefaultLogWatch watch, final LogWatchBuilder builder,
        final TailSplitter splitter) {
        this.watch = watch;
        this.splitter = splitter;
        this.delayBetweenReads = builder.getDelayBetweenReads();
        this.bufferSize = builder.getReadingBufferSize();
        this.reopenBetweenReads = builder.isClosingBetweenReads();
        this.ignoreExistingContent = !builder.isReadingFromBeginning();
    }

    public Message getCurrentlyProcessedMessage() {
        if (this.currentlyProcessedMessage == null) {
            return null;
        } else {
            return this.currentlyProcessedMessage.buildIntermediate(this.splitter);
        }
    }

    public DefaultLogWatch getWatch() {
        return this.watch;
    }

    public synchronized boolean isRunning() {
        return (this.tailer != null);
    }

    protected void readingFinished() {
        if (!this.isReading.compareAndSet(true, false)) {
            return;
        }
        LogWatchTailingManager.LOGGER.info("Tailing stopped submitting lines.");
        if (this.currentlyProcessedMessage != null) {
            /*
             * there will be no more lines from the current reading burst; the
             * currently processed message must be marked as INCOMING with the
             * possibility of being finished in the subsequent reading burst(s).
             */
            this.getWatch().messageIncoming(this.currentlyProcessedMessage.buildIntermediate(this.splitter));
        }
    }

    protected void readingStarted() {
        if (!this.isReading.compareAndSet(false, true)) {
            return;
        }
        LogWatchTailingManager.LOGGER.info("Tailing will now start submitting lines.");
    }

    protected void readLine(final String line) {
        if (!this.isReading.get()) {
            LogWatchTailingManager.LOGGER.warn("Line '{}' received when the tailer shouldn't have been sending: {}.",
                    line, this);
            return;
        }
        final boolean isMessageBeingProcessed = this.currentlyProcessedMessage != null;
        if (this.splitter.isStartingLine(line)) {
            // new message begins
            if (isMessageBeingProcessed) { // finish old message
                LogWatchTailingManager.LOGGER.debug("Existing message will be finished.");
                final Message completeMessage = this.currentlyProcessedMessage.buildFinal(this.splitter);
                final MessageDeliveryStatus accepted = this.getWatch().messageArrived(completeMessage);
                this.currentlyProcessedMessage = null;
                if (accepted == null) {
                    LogWatchTailingManager.LOGGER.info("Message {} rejected at the gate to {}.", completeMessage, this);
                } else if (accepted == MessageDeliveryStatus.ACCEPTED) {
                    this.previousAcceptedMessage = new WeakReference<Message>(completeMessage);
                } else {
                    LogWatchTailingManager.LOGGER
                    .info("Message {} rejected from storage in {}.", completeMessage, this);
                }
            }
            // prepare for new message
            LogWatchTailingManager.LOGGER.debug("New message is being prepared.");
            this.currentlyProcessedMessage = new MessageBuilder(line);
            if (this.previousAcceptedMessage != null) {
                this.currentlyProcessedMessage.setPreviousMessage(this.previousAcceptedMessage.get());
            }
        } else {
            // continue present message
            if (!isMessageBeingProcessed) {
                LogWatchTailingManager.LOGGER.debug("Disregarding line as trash.");
                // most likely just a garbage immediately after start
                return;
            }
            LogWatchTailingManager.LOGGER.debug("Existing message is being updated.");
            this.currentlyProcessedMessage.add(line);
        }
        LogWatchTailingManager.LOGGER.debug("Line processing over.");
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
        final boolean willReadFromEnd = this.willReadFromEnd();
        LogWatchTailingManager.LOGGER.debug("Tailer {} ignore existing file contents.", willReadFromEnd ? "will"
                : "won't");
        this.tailer = new SplitlogTailer(this.watch.getWatchedFile(), new LogWatchTailerListener(this),
                this.delayBetweenReads, this.willReadFromEnd(), this.reopenBetweenReads, this.bufferSize);
        LogWatchTailingManager.EXECUTOR.submit(this.tailer);
        final long start = System.nanoTime();
        this.tailer.waitUntilStarted();
        final long duration = System.nanoTime() - start;
        LogWatchTailingManager.LOGGER.debug("It took {} ms for the tailing to actually start.",
                TimeUnit.NANOSECONDS.toMillis(duration));
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
        /*
         * terminate tailer; we stop the scheduler and the task will therefore
         * never be started again
         */
        this.tailer.stop();
        LogWatchTailingManager.LOGGER.info("Terminated tailing #{} for {}.",
                this.numberOfTimesThatTailerWasStarted.get(), this.watch);
        return true;
    }

    protected void tailingFinished() {
        this.isReading.set(false);
        LogWatchTailingManager.LOGGER.info("Tailing terminated.");
        if (this.currentlyProcessedMessage != null) {
            /*
             * there will be no more lines. the last message must be accepted or
             * rejected as well.
             */
            this.getWatch().messageArrived(this.currentlyProcessedMessage.buildFinal(this.splitter));
            this.currentlyProcessedMessage = null;
            this.previousAcceptedMessage = null;
        }
    }

    private synchronized boolean willReadFromEnd() {
        if (this.numberOfTimesThatTailerWasStarted.get() > 0) {
            return true;
        } else {
            return this.ignoreExistingContent;
        }
    }

}
