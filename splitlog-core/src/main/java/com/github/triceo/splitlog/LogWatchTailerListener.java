package com.github.triceo.splitlog;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LogWatchTailerListener implements TailerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWatchTailerListener.class);

    private final DefaultLogWatch watcher;

    /*
     * TODO ideally this would accept AbstractLogWatch instead, to enable
     * different impls.
     */
    public LogWatchTailerListener(final DefaultLogWatch watcher) {
        this.watcher = watcher;
    }

    @Override
    public void fileNotFound() {
        LogWatchTailerListener.LOGGER.info("Log file not found: {}.", this.watcher.getWatchedFile());
    }

    @Override
    public void fileRotated() {
        LogWatchTailerListener.LOGGER.info("Log file rotated: {}.", this.watcher.getWatchedFile());
    }

    @Override
    public void handle(final Exception ex) {
        LogWatchTailerListener.LOGGER.warn("Exception from the log tailer for file " + this.watcher.getWatchedFile(),
                ex);
    }

    @Override
    public void handle(final String line) {
        LogWatchTailerListener.LOGGER.info("Tailer for {} received line '{}'.", this.watcher.getWatchedFile(), line);
        this.watcher.addLine(line);
    }

    @Override
    public void init(final Tailer tailer) {
        LogWatchTailerListener.LOGGER.info("Tailer initialized for file: {}.", this.watcher.getWatchedFile());
    }

}
