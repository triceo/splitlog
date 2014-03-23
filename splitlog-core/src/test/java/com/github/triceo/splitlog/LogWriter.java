package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;

public class LogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWriter.class);
    private final File target;
    private final ExecutorService e = Executors.newSingleThreadExecutor();

    public LogWriter(final File target) {
        this.target = target;
    }

    public void destroy() {
        this.e.shutdownNow();
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
        this.write(line);
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
                return LogWriter.this.write(line);
            }

        });
        try {
            return result.get();
        } catch (final Exception ex) {
            LogWriter.LOGGER.warn("Failed writing log message '{}'.", line, ex);
            return false;
        }
    }

    private boolean write(final String line) {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(LogWriter.this.target, true));
            w.write(line);
            w.newLine();
        } catch (final IOException ex) {
            return false;
        } finally {
            IOUtils.closeQuietly(w);
        }
        try {
            final Collection<String> lines = IOUtils.readLines(new FileReader(this.target));
            LogWriter.LOGGER.debug("Contents of file '{}': {}.", this.target, lines);
        } catch (final Exception e) {
            LogWriter.LOGGER.warn("File '{}' cannot be read.", this.target, e);
        }
        return true;
    }
}
