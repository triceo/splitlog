package com.github.triceo.splitlog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME this is not good. TailerListener and the message consumption should be two different classes
class LogWatchTailerListener implements TailerListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LogWatchTailerListener.class);
    
    private final Queue<RawMessage> messages = new ConcurrentLinkedQueue<RawMessage>();
    private final TailSplitter splitter;
    private File watchedFile;
    
    public LogWatchTailerListener(final TailSplitter splitter) {
        this.splitter = splitter;
    }
    
    /**
     * Will return a list of all fully processed messages up to this point, since last called.
     * @return
     */
    public List<RawMessage> consumeMessages() {
        List<RawMessage> result = new ArrayList<RawMessage>(messages);
        messages.clear();
        return result;
    }
    
    /**
     * Will return the first fully processed messages since last called.
     * @return Null if there is no fully processed message.
     */
    public RawMessage consumeMessage() {
        return this.messages.poll();
    }
    
    /**
     * Will return a list of all messages up to this point, since last called.
     * @return If there is a message that hasn't yet been fully processed, processing will be terminated and the message will be returned also.
     */
    public List<RawMessage> flush() {
        RawMessage m = splitter.forceProcessing();
        if (m != null) {
            messages.add(m);
        }
        return consumeMessages();
    }

    @Override
    public void init(Tailer tailer) {
        this.watchedFile = tailer.getFile();
    }

    @Override
    public void fileNotFound() {
        LOGGER.info("Log file not found: {}.", this.watchedFile);
    }

    @Override
    public void fileRotated() {
        LOGGER.info("Log file rotated: {}.", this.watchedFile);
    }

    @Override
    public void handle(String line) {
        RawMessage msg = this.splitter.addLine(line);
        if (msg == null) {
            return;
        }
        this.messages.add(msg);
    }

    @Override
    public void handle(Exception ex) {
        LOGGER.warn("Exception from the log tailer.", ex);
    }

}
