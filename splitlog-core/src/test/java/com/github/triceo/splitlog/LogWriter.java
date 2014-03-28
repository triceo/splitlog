package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;

/**
 * Write a message to a given log file.
 * 
 * This class is designed in such a way that it would be literally impossible
 * for two threads to attempt to write the same file at the same time.
 * 
 */
public class LogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWriter.class);
    private static final Map<File, LogWriter> WRITERS = new HashMap<File, LogWriter>();
    private final File target;
    private boolean isDestroyed = false;
    private final ExecutorService e = Executors.newSingleThreadExecutor();

    public static synchronized LogWriter forFile(final File f) {
        if (!LogWriter.WRITERS.containsKey(f)) {
            LogWriter.WRITERS.put(f, new LogWriter(f));
        }
        return LogWriter.WRITERS.get(f);
    }

    private LogWriter(final File target) {
        this.target = target;
    }

    public synchronized void destroy() {
        LogWriter.WRITERS.remove(this.target);
        this.e.shutdownNow();
        this.isDestroyed = true;
    }

    /**
     * Writes a line to the log and waits until the follower receives it.
     * 
     * @param line
     *            Message to write.
     * @param follower
     *            Tailer to wait for the message.
     * @return The line that was written, or null otherwise.
     */
    public String write(final String line, final Follower follower) {
        if (!this.writeWithoutWaiting(line, follower)) {
            LogWriter.LOGGER.debug("Write failed: '{}'.", line);
            return null;
        }
        // wait until the last part of the string is finally present
        final Message result = follower.waitFor(new MessageDeliveryCondition() {

            private boolean accept(final Message receivedMessage, final MessageDeliveryStatus status) {
                final String lastLine = receivedMessage.getLines().get(receivedMessage.getLines().size() - 1);
                final String textStr[] = line.split("\\r?\\n");
                return (textStr[textStr.length - 1].trim().equals(lastLine.trim()));
            }

            @Override
            public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
                return this.accept(evaluate, status);
            }

            @Override
            public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final Follower source) {
                return this.accept(evaluate, status);
            }

        }, 10, TimeUnit.SECONDS);
        if (result == null) {
            throw new IllegalStateException("No message received in time.");
        }
        return result.getLines().get(result.getLines().size() - 1);
    }

    public void writeWithoutWaiting(final String line) {
        this.actuallyWrite(line);
    }

    /**
     * Writes a message to the log.
     * 
     * @param line
     *            Message to write.
     * @param follower
     *            Tailer to wait for the message.
     * @return If the message has been written.
     */
    private boolean writeWithoutWaiting(final String line, final Follower follower) {
        final Future<Boolean> result = this.e.submit(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                return LogWriter.this.actuallyWrite(line);
            }

        });
        try {
            return result.get();
        } catch (final Exception ex) {
            LogWriter.LOGGER.warn("Failed writing log message '{}'.", line, ex);
            return false;
        }
    }

    private synchronized boolean actuallyWrite(final String line) {
        if (this.isDestroyed) {
            return false;
        }
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(this.target, true));
            w.write(line);
            w.newLine();
        } catch (final IOException ex) {
            return false;
        } finally {
            IOUtils.closeQuietly(w);
        }
        return true;
    }
}
