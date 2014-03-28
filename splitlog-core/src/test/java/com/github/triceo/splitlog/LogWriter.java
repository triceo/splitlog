package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryCondition;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;

/**
 * Write a message to a given log file.
 * 
 * This class is designed in such a way that it would be literally impossible
 * for two threads to attempt to write the same file at the same time.
 * 
 */
public class LogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWriter.class);
    /**
     * This provides exclusive access to each file through
     * {@link #forFile(File)}.
     */
    private static final Map<File, LogWriter> WRITERS = new HashMap<File, LogWriter>();
    /**
     * The longer this delay, the less likely that a message will be missed by
     * the subsequent waitFor() calls.
     */
    private static final long DELAY_BEFORE_WRITE_MILLIS = 100;
    /**
     * The longer this delay, the less likely will a delay in I/O break this
     * test.
     */
    private static final long WAIT_FOR_MESSAGE_MILLIS = LogWriter.DELAY_BEFORE_WRITE_MILLIS * 100;
    private final File target;
    private boolean isDisposed = false;
    private final ScheduledExecutorService e = Executors.newScheduledThreadPool(1);

    public static synchronized LogWriter forFile(final File f) {
        if (!LogWriter.WRITERS.containsKey(f)) {
            LogWriter.WRITERS.put(f, new LogWriter(f));
        }
        return LogWriter.WRITERS.get(f);
    }

    private LogWriter(final File target) {
        this.target = target;
    }

    public synchronized void dispose() {
        LogWriter.WRITERS.remove(this.target);
        this.e.shutdownNow();
        this.isDisposed = true;
    }

    /**
     * Writes a line to the log and waits until the follower receives it.
     * 
     * @param line
     *            Message to write.
     * @param follower
     *            Follower to wait for the message.
     * @return The line that was written, or null otherwise.
     */
    public String write(final String line, final Follower follower) {
        this.writeDelayed(line);
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

        }, LogWriter.WAIT_FOR_MESSAGE_MILLIS, TimeUnit.MILLISECONDS);
        if (result == null) {
            throw new IllegalStateException("No message received in time.");
        }
        return result.getLines().get(result.getLines().size() - 1);
    }

    /**
     * Write message to a file on background.
     * 
     * This method will schedule the write operation, but it will not actually
     * be ececuted until {@link #DELAY_BEFORE_WRITE_MILLIS} milliseconds later.
     * This is so that the subsequent
     * {@link CommonFollower#waitFor(MessageDeliveryCondition)}s have a chance
     * to be registered.
     * 
     * @param line
     *            Message to write.
     */
    private synchronized void writeDelayed(final String line) {
        LogWriter.LOGGER.info("Delayed write of '{}' to {} scheduled.", line, LogWriter.this.target);
        this.e.schedule(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                LogWriter.LOGGER.info("Delayed write of '{}' to {} executing now.", line, LogWriter.this.target);
                return LogWriter.this.writeNow(line);
            }

        }, LogWriter.DELAY_BEFORE_WRITE_MILLIS, TimeUnit.MILLISECONDS);
    }

    public synchronized boolean writeNow(final String line) {
        if (this.isDisposed) {
            LogWriter.LOGGER.info("Not writing '{}' into {} as the writer is already disposed.", line, this.target);
            return false;
        }
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(this.target, true));
            w.write(line);
            w.newLine();
            w.flush();
            LogWriter.LOGGER.info("Written '{}' into {}.", line, this.target);
        } catch (final IOException ex) {
            LogWriter.LOGGER.warn("Failed writing '{}' into {}.", line, this.target, ex);
            return false;
        } finally {
            IOUtils.closeQuietly(w);
        }
        return true;
    }
}
