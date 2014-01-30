package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

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
