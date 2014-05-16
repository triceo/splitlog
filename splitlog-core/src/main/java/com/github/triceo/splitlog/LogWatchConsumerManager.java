package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageListener;

/**
 * On top of the typical consumer manager, this is also tasked with starting and
 * stopping the tailing whenever it is informed by the {@link LogWatch} that it
 * is possible.
 *
 */
final class LogWatchConsumerManager extends ConsumerManager<LogWatch> {

    private final LogWatchSweepingManager sweeping;
    private final LogWatchTailingManager tailing;

    public LogWatchConsumerManager(final LogWatchBuilder builder, final LogWatchStorageManager messageStore,
        final DefaultLogWatch watch) {
        super(watch);
        this.tailing = new LogWatchTailingManager(watch, builder);
        this.sweeping = new LogWatchSweepingManager(messageStore, builder.getDelayBetweenSweeps());
    }

    @Override
    public synchronized boolean registerConsumer(final MessageConsumer<LogWatch> consumer) {
        final boolean result = super.registerConsumer(consumer);
        this.startTailing(consumer);
        return result;
    }

    @Override
    public synchronized MessageConsumer<LogWatch> startConsuming(final MessageListener<LogWatch> listener) {
        final MessageConsumer<LogWatch> result = super.startConsuming(listener);
        this.startTailing(listener);
        return result;
    }

    private synchronized void startTailing(final MessageListener<LogWatch> watch) {
        if (!(watch instanceof Follower)) {
            /*
             * only start the threads if we're actually capable of receiving
             * messages
             */
            return;
        }
        this.sweeping.start();
        this.tailing.start();
    }

    @Override
    public synchronized boolean stop() {
        final boolean result = super.stop();
        this.sweeping.stop();
        return result;
    }

    @Override
    public synchronized boolean stopConsuming(final MessageConsumer<LogWatch> consumer) {
        final boolean result = super.stopConsuming(consumer);
        if ((consumer instanceof Follower) && (this.countConsumers() < 1)) {
            this.tailing.stop();
        }
        return result;
    }

}
