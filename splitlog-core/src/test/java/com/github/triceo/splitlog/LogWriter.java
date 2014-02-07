package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.LineCondition;

class LogWriter {

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
     * Writes a line to the log and waits until the tailer receives it.
     * 
     * @param line
     *            Message to write.
     * @param tailer
     *            Tailer to wait for the message.
     * @return If the message has been written.
     */
    public boolean write(final String line, final AbstractLogTailer tailer) {
        if (!this.writeWithoutWaiting(line, tailer)) {
            return false;
        }
        // wait until the last part of the string is finally present
        return tailer.waitFor(new LineCondition() {

            public boolean accept(final String receivedLine) {
                final String textStr[] = line.split("\\r?\\n");
                return (textStr[textStr.length - 1].trim().equals(receivedLine.trim()));
            }

        }, 10, TimeUnit.SECONDS);
    }

    /**
     * Writes a message to the log.
     * 
     * @param line
     *            Message to write.
     * @param tailer
     *            Tailer to wait for the message.
     * @return If the message has been written.
     */
    private boolean writeWithoutWaiting(final String line, final AbstractLogTailer tailer) {
        final Future<Boolean> result = this.e.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                BufferedWriter w = null;
                try {
                    w = new BufferedWriter(new FileWriter(LogWriter.this.target, true));
                    w.write(line);
                    w.newLine();
                    LogWriter.LOGGER.info("Written log message '{}'.", line);
                    return true;
                } catch (final IOException ex) {
                    LogWriter.LOGGER.warn("Failed writing log message '{}'.", line, ex);
                    return false;
                } finally {
                    IOUtils.closeQuietly(w);
                }
            }

        });
        try {
            return result.get();
        } catch (final Exception ex) {
            LogWriter.LOGGER.warn("Failed writing log message '{}'.", line, ex);
            return false;
        }
    }

}
