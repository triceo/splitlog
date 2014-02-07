package com.github.triceo.splitlog;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.LineCondition;
import com.github.triceo.splitlog.conditions.MessageCondition;

/**
 * This is a log tailer that holds no message data, just the tags. For message
 * data, it will always turn to the underlying {@link LogWatch}.
 * 
 * This class assumes that LogWatch and user code are the only two threads that
 * use it. Never use one instance of this class from two or more user threads.
 * Otherwise, expect unpredictable behavior from waitFor() methods.
 */
class NonStoringLogTailer extends AbstractLogTailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringLogTailer.class);

    private MessageCondition messageBlockingCondition = null;
    private LineCondition lineBlockingCondition = null;
    // FIXME this assumes that only the tail thread and user thread are present
    private CyclicBarrier blocker;

    private final Map<Integer, Message> tags = new TreeMap<Integer, Message>();

    public NonStoringLogTailer(final LogWatch watch) {
        super(watch);
        this.refreshBarrier();
    }

    @Override
    public List<Message> getMessages() {
        final List<Message> messages = new LinkedList<Message>(this.getWatch().getAllMessages(this));
        int offset = 0;
        for (final Map.Entry<Integer, Message> entry : this.tags.entrySet()) {
            messages.add(entry.getKey() + offset, entry.getValue());
            offset++;
        }
        return messages;
    }

    @Override
    protected void notifyOfLine(final String line) {
        if (this.lineBlockingCondition == null) {
            return;
        } else if (this.lineBlockingCondition.accept(line)) {
            try {
                this.blocker.await();
            } catch (final Exception e) {
                this.blocker.reset();
            } finally {
                this.lineBlockingCondition = null;
                this.refreshBarrier();
            }
        }
    }

    @Override
    protected void notifyOfMessage(final Message msg) {
        if (this.messageBlockingCondition == null) {
            return;
        } else if (this.messageBlockingCondition.accept(msg)) {
            try {
                this.blocker.await();
            } catch (final Exception e) {
                this.blocker.reset();
            } finally {
                this.messageBlockingCondition = null;
                this.refreshBarrier();
            }
        }
    }

    private void refreshBarrier() {
        this.blocker = new CyclicBarrier(2);
    }

    /**
     * See {@link AbstractLogTailer#tag(String)}. Subsequent tags at the same
     * locations will overwrite each other.
     */
    @Override
    public void tag(final String tagLine) {
        final int messageId = this.getWatch().getAllMessages(this).size();
        final Message message = new Message(tagLine);
        this.tags.put(messageId, message);
    }

    @Override
    public boolean waitFor(final LineCondition condition) {
        this.lineBlockingCondition = condition;
        return this.waitFor(-1, TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean waitFor(final LineCondition condition, final long timeout, final TimeUnit unit) {
        this.lineBlockingCondition = condition;
        return this.waitFor(timeout, unit);
    }

    private boolean waitFor(final long timeout, final TimeUnit unit) {
        try {
            if (timeout < 0) {
                this.blocker.await();
            } else {
                this.blocker.await(timeout, unit);
            }
            return true;
        } catch (final InterruptedException e) {
            NonStoringLogTailer.LOGGER.warn("Waiting interrupted.", e);
            return false;
        } catch (final BrokenBarrierException e) {
            NonStoringLogTailer.LOGGER.warn("Waiting aborted.", e);
            return false;
        } catch (final TimeoutException e) {
            NonStoringLogTailer.LOGGER.info("Waiting ended in a timeout.");
            return false;
        }
    }

    @Override
    public boolean waitFor(final MessageCondition condition) {
        this.messageBlockingCondition = condition;
        return this.waitFor(-1, TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean waitFor(final MessageCondition condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        this.messageBlockingCondition = condition;
        return this.waitFor(timeout, unit);
    }
}
