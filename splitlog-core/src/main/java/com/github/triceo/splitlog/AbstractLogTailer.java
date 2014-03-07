package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.github.triceo.splitlog.conditions.MessageCondition;

/**
 * Internal API for a log tailer that, on top of the public API, provides ways
 * of notifying the tailer of new messages. Every tailer, such as
 * {@link NonStoringLogTailer}, needs to extend this class.
 * 
 */
abstract class AbstractLogTailer implements LogTailer {

    private static final class AllAcceptingMessageCondition implements MessageCondition {
        @Override
        public boolean accept(final Message evaluate) {
            // accept every message
            return true;
        }
    }

    private final DefaultLogWatch watch;

    protected AbstractLogTailer(final DefaultLogWatch watch) {
        this.watch = watch;
    }

    @Override
    public List<Message> getMessages() {
        return this.getMessages(new AllAcceptingMessageCondition());
    }

    protected DefaultLogWatch getWatch() {
        return this.watch;
    }

    @Override
    public boolean isTerminated() {
        return this.watch.isTerminated(this);
    }

    /**
     * Notify the tailer of a new line in the watched log. Must never be called
     * by users, just from the library code.
     * 
     * @param line
     *            The line.
     */
    protected abstract void notifyOfLine(String line);

    /**
     * Notify the tailer of a new message in the watched log. Must never be
     * called by users, just from the library code.
     * 
     * @param msg
     *            The message.
     */
    protected abstract void notifyOfMessage(Message msg);

    @Override
    public boolean write(final OutputStream stream) {
        return this.write(stream, new AllAcceptingMessageCondition());
    }

    @Override
    public boolean write(final OutputStream stream, final MessageCondition condition) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream may not be null.");
        } else if (condition == null) {
            throw new IllegalArgumentException("Condition may not be null.");
        }
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            for (final Message msg : this.getMessages(condition)) {
                w.write(msg.toString().trim());
                w.newLine();
            }
            return true;
        } catch (final IOException ex) {
            return false;
        } finally {
            IOUtils.closeQuietly(w);
        }
    }

}
