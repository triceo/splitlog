package com.github.triceo.splitlog;

import java.io.File;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogWatchTailerListener implements TailerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWatchTailerListener.class);

    private final DefaultLogWatch watcher;
    private File watchedFile;

    /*
     *  TODO ideally this would accept AbstractLogWatch instead, to enable
     *  different impls.
     */
    public LogWatchTailerListener(final DefaultLogWatch watcher) {
        this.watcher = watcher;
    }

    @Override
    public void fileNotFound() {
        LogWatchTailerListener.LOGGER.info("Log file not found: {}.", this.watchedFile);
    }

    @Override
    public void fileRotated() {
        LogWatchTailerListener.LOGGER.info("Log file rotated: {}.", this.watchedFile);
    }

    @Override
    public void handle(final Exception ex) {
        LogWatchTailerListener.LOGGER.warn("Exception from the log tailer.", ex);
    }

    @Override
    public void handle(final String line) {
        this.watcher.addLine(line);
        LogWatchTailerListener.LOGGER.debug("Tailer handled message: '{}'.", line);
    }

    @Override
    public void init(final Tailer tailer) {
        this.watchedFile = tailer.getFile();
        LogWatchTailerListener.LOGGER.info("Tailer initialized for file: {}.", this.watchedFile);
    }

}
