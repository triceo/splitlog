package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

abstract class AbstractLogTailer implements Tailable {

    private final LogWatch watch;

    protected AbstractLogTailer(final LogWatch watch) {
        this.watch = watch;
    }

    public abstract List<Message> getMessages();

    protected LogWatch getWatch() {
        return this.watch;
    }

    public boolean isTerminated() {
        return this.watch.isTailing(this);
    }

    protected abstract void notifyOfMessage(Message msg);

    public AbstractLogTailer startTailing() {
        return this.watch.startTailing();
    }

    public boolean terminateTailing() {
        return this.watch.terminateTailing(this);
    }

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

    public boolean write(final OutputStream stream) {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(stream));
            for (final Message msg : this.getMessages()) {
                w.write(msg.getRawMessage().toString());
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
