package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;

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

    public synchronized static File createTempFile() {
        try {
            final File targetFolder = new File("target/tmp/");
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            return File.createTempFile("test-", null, targetFolder);
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temp files.", e);
        }
    }

    public static synchronized LogWriter forFile(final File f) {
        if (!LogWriter.WRITERS.containsKey(f)) {
            LogWriter.LOGGER.info("Creating new LogWriter for {}.", f);
            LogWriter.WRITERS.put(f, new LogWriter(f));
        }
        return LogWriter.WRITERS.get(f);
    }

    private boolean isDisposed = false;

    private final File target;

    private LogWriter(final File target) {
        this.target = target;
    }

    public synchronized void dispose() {
        LogWriter.WRITERS.remove(this.target);
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
        final Future<Message> future = follower.expect(new MidDeliveryMessageCondition<LogWatch>() {

            @Override
            public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
                final List<String> lines = evaluate.getLines();
                final String lastLine = lines.get(lines.size() - 1);
                final String textStr[] = line.split("\\r?\\n");
                return (textStr[textStr.length - 1].trim().equals(lastLine.trim()));
            }

        });
        if (!this.writeNow(line)) {
            throw new IllegalStateException("Failed writing message.");
        }
        try {
            // wait until the last part of the string is finally present
            final Message result = future.get();
            final List<String> lines = result.getLines();
            return lines.get(lines.size() - 1);
        } catch (final Exception e) {
            throw new IllegalStateException("No message received in time.", e);
        }
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
            LogWriter.LOGGER.info("Written '{}' into {}.", line, this.target);
        } catch (final IOException ex) {
            LogWriter.LOGGER.warn("Failed writing '{}' into {}.", line, this.target, ex);
            return false;
        } finally {
            IOUtils.closeQuietly(w);
            LogWriter.LOGGER.debug("File closed: {}.", this.target);
        }
        return true;
    }
}
