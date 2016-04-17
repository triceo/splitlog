package com.github.triceo.splitlog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
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
        try {
            FileUtils.writeStringToFile(target, line + "\n", "UTF-8", true);
            LogWriter.LOGGER.info("Written '{}' into {}.", line, target);
            return true;
        } catch (final IOException ex) {
            LogWriter.LOGGER.error("Failed writing '{}' into {}.", line, target, ex);
            return false;
        }
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
        final Future<Message> future = follower.expect((evaluate, status, source) -> {
            final List<String> lines = evaluate.getLines();
            final String lastLine = lines.get(lines.size() - 1);
            final String textStr[] = line.split("\\r?\\n");
            return (textStr[textStr.length - 1].trim().equals(lastLine.trim()));
        });
        if (!LogWriter.write(follower.getFollowed().getWatchedFile(), line)) {
            throw new IllegalStateException("Failed writing message.");
        } else if (follower.isStopped()) {
            throw new IllegalStateException("Follower cannot receive message as it is already stopped.");
        }
        try {
            // wait until the last part of the string is finally present
            LogWriter.LOGGER.info("Waiting for '{}' starting in {}.", line, follower);
            final Message result = future.get(10, TimeUnit.SECONDS);
            final List<String> lines = result.getLines();
            return lines.get(lines.size() - 1);
        } catch (final Exception e) {
            throw new IllegalStateException("No message received in time.", e);
        }
    }

}
