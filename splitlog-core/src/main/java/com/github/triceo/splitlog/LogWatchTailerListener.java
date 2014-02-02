package com.github.triceo.splitlog;

import java.io.File;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogWatchTailerListener implements TailerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWatchTailerListener.class);

    private final TailSplitter splitter;
    private final LogWatch watcher;
    private File watchedFile;

    public LogWatchTailerListener(final LogWatch watcher, final TailSplitter splitter) {
        this.splitter = splitter;
        this.watcher = watcher;
    }

    public void fileNotFound() {
        LogWatchTailerListener.LOGGER.info("Log file not found: {}.", this.watchedFile);
    }

    public void fileRotated() {
        LogWatchTailerListener.LOGGER.info("Log file rotated: {}.", this.watchedFile);
    }

    public void handle(final Exception ex) {
        LogWatchTailerListener.LOGGER.warn("Exception from the log tailer.", ex);
    }

    public void handle(final String line) {
        final Message msg = this.splitter.addLine(line);
        if (msg != null) {
            this.watcher.addMessage(msg);
        }
    }

    public void init(final Tailer tailer) {
        this.watchedFile = tailer.getFile();
    }

}
