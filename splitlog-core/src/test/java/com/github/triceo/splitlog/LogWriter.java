package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 */
public class LogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWriter.class);

    public synchronized static File createTempFile() {
        try {
            return File.createTempFile("test-", null);
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temp files.", e);
        }
    }

    public static boolean write(final File target, final String line) {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(target, true));
            w.write(line);
            w.newLine();
            LogWriter.LOGGER.info("Written '{}' into {}.", line, target);
        } catch (final IOException ex) {
            LogWriter.LOGGER.warn("Failed writing '{}' into {}.", line, target, ex);
            return false;
        } finally {
            IOUtils.closeQuietly(w);
            LogWriter.LOGGER.debug("File closed: {}.", target);
        }
        return true;
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
    public static String write(final Follower follower, final String line) {
        final File target = follower.getFollowed().getWatchedFile();
        final Future<Message> future = follower.expect(new MidDeliveryMessageCondition<LogWatch>() {

            @Override
            public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
                final List<String> lines = evaluate.getLines();
                final String lastLine = lines.get(lines.size() - 1);
                final String textStr[] = line.split("\\r?\\n");
                return (textStr[textStr.length - 1].trim().equals(lastLine.trim()));
            }

        });
        if (!LogWriter.write(target, line)) {
            throw new IllegalStateException("Failed writing message.");
        }
        try {
            // wait until the last part of the string is finally present
            final Message result = future.get(10, TimeUnit.SECONDS);
            final List<String> lines = result.getLines();
            return lines.get(lines.size() - 1);
        } catch (final Exception e) {
            throw new IllegalStateException("No message received in time.", e);
        }
    }

}
