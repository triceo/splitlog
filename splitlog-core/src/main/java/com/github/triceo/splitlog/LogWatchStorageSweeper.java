package com.github.triceo.splitlog;

import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

/**
 * As followers are terminated via {@link #stopFollowing(Follower)} and then
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
final class LogWatchStorageSweeper implements Runnable {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(LogWatchStorageSweeper.class);
    private final LogWatchStorageManager toSweep;

    public LogWatchStorageSweeper(final LogWatchStorageManager watch) {
        this.toSweep = watch;
    }

    @Override
    public void run() {
        final MessageStore messages = this.toSweep.getMessageStore();
        final int minId = this.toSweep.getFirstReachableMessageId();
        LogWatchStorageSweeper.LOGGER.debug(
                "Starting message sweep from {}. First reachable message ID reportedly {}.",
                this.toSweep.getLogWatch(), minId);
        if (minId < 0) {
            LogWatchStorageSweeper.LOGGER.info("Sweeping all messages from {} as none are reachable.",
                    this.toSweep.getLogWatch());
            messages.discardBefore(messages.getNextPosition());
            return;
        } else if (messages.isEmpty()) {
            LogWatchStorageSweeper.LOGGER.info("No messages in {}.", this.toSweep.getLogWatch());
            return;
        }
        final int num = messages.discardBefore(minId);
        LogWatchStorageSweeper.LOGGER.info("Swept {} messages from {}.", num, this.toSweep.getLogWatch());
    }

}