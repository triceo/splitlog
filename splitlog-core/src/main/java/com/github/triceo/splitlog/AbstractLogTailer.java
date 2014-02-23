package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.github.triceo.splitlog.conditions.LineCondition;
import com.github.triceo.splitlog.conditions.MessageCondition;

public abstract class AbstractLogTailer {

    private final LogWatch watch;

    protected AbstractLogTailer(final LogWatch watch) {
        this.watch = watch;
    }

    /**
     * Retrieve messages that this tailer has been notified of, including tags
     * 
     * @return Messages we are aware of, in their original order.
     */
    public List<Message> getMessages() {
        return this.getMessages(new MessageCondition() {

            @Override
            public boolean accept(final Message evaluate) {
                // accept every message
                return true;
            }

        });
    }

    /**
     * Retrieve messages that this tailer has been notified of, if a certain
     * condition holds true for them, including tags.
     * 
     * @param condition
     *            The condition.
     * @return Every message we are aware of, for which the condition holds true.
     */
    public abstract List<Message> getMessages(final MessageCondition condition);

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
     * @return Null if the method unblocked due to some other reason.
     */
    public abstract String waitFor(LineCondition condition);

    /**
     * Will block until a line appears in the log, for which the condition is
     * true. If none appears before the timeout, it unblocks anyway.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Null if the method unblocked due to some other reason.
     */
    public abstract String waitFor(LineCondition condition, long timeout, TimeUnit unit);

    /**
     * Will block until a message arrives, for which the condition is true.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Null if the method unblocked due to some other reason.
     */
    public abstract Message waitFor(MessageCondition condition);

    /**
     * Will block until a message arrives, for which the condition is true. If
     * none arrives before the timeout, it unblocks anyway.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Null if the method unblocked due to some other reason.
     */
    public abstract Message waitFor(MessageCondition condition, long timeout, TimeUnit unit);

    /**
     * Will write to a stream that which is be returned by
     * {@link #getMessages()}.
     * 
     * @param stream
     *            Target.
     * @return True if written, false otherwise.
     */
    public boolean write(final OutputStream stream) {
        return this.write(stream, new MessageCondition() {

            @Override
            public boolean accept(final Message msg) {
                // accept every message
                return true;
            }

        });
    }

    /**
     * Will write to a stream that which is be returned by
     * {@link #getMessages(MessageCondition)}.
     * 
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(MessageCondition)}.
     * @return True if written, false otherwise.
     */
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
