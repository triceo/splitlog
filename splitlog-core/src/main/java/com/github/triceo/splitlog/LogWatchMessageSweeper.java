package com.github.triceo.splitlog;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * As followers are terminated via {@link #unfollow(Follower)} and then
 * discarded by GC, some messages will become unreachable. (No followers point
 * to them any longer.)
 * 
 * The point of this class is to review the situation and discard whatever
 * messages are no longer reachable. Otherwise they'd be uselessly occupying
 * memory.
 * 
 * This class is intended to be run periodically by
 * {@link ScheduledExecutorService}.
 */
class LogWatchMessageSweeper implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWatchMessageSweeper.class);
    /**
     * The watchToSweep to log watch is weak, so that we prevent any possibility
     * of watches from being reclaimed by the GC when they're no longer used by
     * client code.
     */
    private final WeakReference<DefaultLogWatch> watchToSweep;

    public LogWatchMessageSweeper(final DefaultLogWatch watch) {
        this.watchToSweep = new WeakReference<DefaultLogWatch>(watch);
    }

    @Override
    public void run() {
        final DefaultLogWatch w = this.watchToSweep.get();
        if (w == null) {
            /*
             * if the cleanup is still scheduled and the logwatch is already
             * gone, terminate. the outside code should try to prevent this,
             * though, by terminating the scheduler before disposing of the
             * logwatch.
             */
            return;
        }
        final MessageStore messages = w.getMessageStore();
        final int minId = w.getFirstReachableMessageId();
        LogWatchMessageSweeper.LOGGER.debug(
                "Starting message sweep from log watch for file '{}'. First reachable message ID reportedly {}.",
                w.watchedFile, minId);
        if (minId < 0) {
            LogWatchMessageSweeper.LOGGER.info(
                    "Sweeping all messages from log watch for file '{}' as none are reachable.", w.watchedFile);
            messages.discardBefore(messages.getNextMessageId());
            return;
        } else if (messages.isEmpty()) {
            LogWatchMessageSweeper.LOGGER.info("No messages in the log watch for file '{}'.", w.watchedFile);
            return;
        }
        final int num = messages.discardBefore(minId);
        LogWatchMessageSweeper.LOGGER.info("Swept {} messages from log watch for file '{}'.", num, w.watchedFile);
    }

}