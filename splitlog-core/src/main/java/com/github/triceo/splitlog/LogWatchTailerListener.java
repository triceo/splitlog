package com.github.triceo.splitlog;

import org.apache.commons.io.input.fork.Tailer;
import org.apache.commons.io.input.fork.TailerListener;
import org.slf4j.Logger;

import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

final class LogWatchTailerListener implements TailerListener {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(LogWatchTailerListener.class);

    private final LogWatchTailingManager manager;

    public LogWatchTailerListener(final LogWatchTailingManager watcher) {
        this.manager = watcher;
    }

    @Override
    public void begin() {
        this.manager.readingStarted();
    }

    @Override
    public void commit() {
        this.manager.readingFinished();
    }

    @Override
    public void destroy() {
        this.manager.tailingFinished();
    }

    @Override
    public void fileNotFound() {
        LogWatchTailerListener.LOGGER.info("Log file not found: {}.", this.manager.getWatch().getWatchedFile());
    }

    @Override
    public void fileRotated() {
        LogWatchTailerListener.LOGGER.info("Log file rotated: {}.", this.manager.getWatch().getWatchedFile());
    }

    @Override
    public void handle(final Exception ex) {
        LogWatchTailerListener.LOGGER.warn("Exception from the log tailer for file: {}.", this.manager.getWatch()
                .getWatchedFile(), ex);
    }

    @Override
    public void handle(final String line) {
        LogWatchTailerListener.LOGGER.info("Tailer for {} received line '{}'.", this.manager.getWatch()
                .getWatchedFile(), line);
        this.manager.readLine(line);
    }

    @Override
    public void init(final Tailer tailer) {
        LogWatchTailerListener.LOGGER.info("Tailer {} initialized for file: {}.", tailer, this.manager.getWatch()
                .getWatchedFile());
    }

}
