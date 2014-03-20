package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.github.triceo.splitlog.conditions.AllMessagesAcceptingCondition;
import com.github.triceo.splitlog.conditions.MessageCondition;

/**
 * Internal API for a log follower that, on top of the public API, provides ways
 * for {@link LogWatch} of notifying the follower of new messages. Every
 * follower implementation, such as {@link NonStoringFollower}, needs to extend
 * this class.
 */
abstract class AbstractFollower implements Follower {

    private final DefaultLogWatch watch;

    protected AbstractFollower(final DefaultLogWatch watch) {
        this.watch = watch;
    }

    @Override
    public List<Message> getMessages() {
        return this.getMessages(AllMessagesAcceptingCondition.INSTANCE);
    }

    protected DefaultLogWatch getWatch() {
        return this.watch;
    }

    @Override
    public boolean isFollowing() {
        return this.watch.isFollowing(this);
    }

    /**
     * Notify the follower that it has been terminated before a message could be
     * delivered completely. Must never be called by users, just from the
     * library code.
     * 
     * @param msg
     *            The message.
     */
    protected abstract void notifyOfUndeliveredMessage(Message msg);

    /**
     * Notify the follower of a new line in the watched log. Must never be
     * called by users, just from the library code.
     * 
     * @param msg
     *            The message.
     */
    protected abstract void notifyOfIncomingMessage(Message msg);

    /**
     * Notify the follower of a new message in the watched log. Must never be
     * called by users, just from the library code.
     * 
     * @param msg
     *            The message.
     */
    protected abstract void notifyOfAcceptedMessage(Message msg);

    /**
     * Notify the follower of a new message from the log that was rejected from
     * entering the log watch. Must never be called by users, just from the
     * library code.
     * 
     * @param msg
     *            The message.
     */
    protected abstract void notifyOfRejectedMessage(Message msg);

    @Override
    public boolean write(final OutputStream stream) {
        return this.write(stream, AllMessagesAcceptingCondition.INSTANCE);
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
