package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.github.triceo.splitlog.conditions.BooleanCondition;
import com.github.triceo.splitlog.conditions.LineCondition;
import com.github.triceo.splitlog.conditions.MessageCondition;

public abstract class AbstractLogTailer {

    private final LogWatch watch;

    protected AbstractLogTailer(final LogWatch watch) {
        this.watch = watch;
    }

    /**
     * Retrieve messages that this tailer has been notified of.
     * 
     * @return Messages we are aware of, in their original order.
     */
    public abstract List<Message> getMessages();

    protected LogWatch getWatch() {
        return this.watch;
    }

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

    /**
     * Mark the current location in the tail by a custom message. It is up to
     * the implementors to decide whether or not a tag inserted at the same time
     * twice is inserted twice, or the second tag overwrites the first.
     * 
     * @param tagLine
     *            Text of the message.
     */
    public abstract void tag(String tagLine);

    /**
     * Will block until a line appears in the log, for which the condition is
     * true.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Whether the line actually appeared, or the method unblocked due
     *         to some other reason.
     */
    public abstract boolean waitFor(LineCondition condition);

    /**
     * Will block until a line appears in the log, for which the condition is
     * true. If none appears before the timeout, it unblocks anyway.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Whether the line actuall appeared, or the method unblocked due to
     *         some other reason.
     */
    public abstract boolean waitFor(LineCondition condition, long timeout, TimeUnit unit);

    /**
     * Will block until a message arrives, for which the condition is true.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Whether the message actually arrived, or the method unblocked due
     *         to some other reason.
     */
    public abstract boolean waitFor(MessageCondition condition);

    /**
     * Will block until a message arrives, for which the condition is true. If
     * none arrives before the timeout, it unblocks anyway.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Whether the message actually arrived, or the method unblocked due
     *         to some other reason.
     */
    public abstract boolean waitFor(MessageCondition condition, long timeout, TimeUnit unit);

    /**
     * Will output all the messages, including tags, into a stream.
     * 
     * @param stream
     *            Target.
     * @return True if written, false otherwise.
     */
    public boolean write(final OutputStream stream) {
        return this.write(stream, new BooleanCondition<Message>() {

            public boolean accept(final Message msg) {
                // accept every message
                return true;
            }

        });
    }

    /**
     * Will output the messages, including tags, that meet a specific condition,
     * into a stream.
     * 
     * @param stream
     *            Target.
     * @param condition
     *            When this object's {@link MessageCondition#accept(Message)}
     *            returns true on a message, the message will be included in the
     *            stream.
     * @return True if written, false otherwise.
     */
    public boolean write(final OutputStream stream, final BooleanCondition<Message> condition) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream may not be null.");
        } else if (condition == null) {
            throw new IllegalArgumentException("Condition may not be null.");
        }
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            for (final Message msg : this.getMessages()) {
                if (!condition.accept(msg)) {
                    continue;
                }
                w.write(msg.toString());
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
