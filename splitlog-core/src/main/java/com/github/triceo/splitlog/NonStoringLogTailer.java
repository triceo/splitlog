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

class NonStoringLogTailer extends AbstractLogTailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringLogTailer.class);

    private MessageCondition blockingCondition = null;
    private CyclicBarrier blocker = new CyclicBarrier(2);

    private final Map<Integer, Message> tags = new TreeMap<Integer, Message>();

    public NonStoringLogTailer(final LogWatch watch) {
        super(watch);
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
    protected void notifyOfMessage(final Message msg) {
        if (this.blockingCondition == null) {
            return;
        } else if (this.blockingCondition.accept(msg)) {
            try {
                this.blocker.await();
            } catch (final Exception e) {
                this.blocker.reset();
            } finally {
                this.refreshBarrier();
            }
        }
    }

    private void refreshBarrier() {
        this.blockingCondition = null;
        this.blocker = new CyclicBarrier(2);
    }

    @Override
    public void tag(final String tagLine) {
        final int messageId = this.getWatch().getAllMessages(this).size();
        final Message message = new Message(tagLine);
        this.tags.put(messageId, message);
    }

    @Override
    public boolean waitFor(final MessageCondition condition) {
        this.blockingCondition = condition;
        try {
            this.blocker.await();
            return true;
        } catch (final Exception e) {
            NonStoringLogTailer.LOGGER.warn("Waiting for message aborted.", e);
            return false;
        } finally {
            this.refreshBarrier();
        }
    }

    @Override
    public boolean waitFor(final MessageCondition condition, final long timeout, final TimeUnit unit) {
        this.blockingCondition = condition;
        try {
            this.blocker.await(timeout, unit);
            return true;
        } catch (final InterruptedException e) {
            NonStoringLogTailer.LOGGER.warn("Waiting for message aborted.", e);
            return false;
        } catch (final BrokenBarrierException e) {
            NonStoringLogTailer.LOGGER.warn("Waiting for message aborted.", e);
            return false;
        } catch (final TimeoutException e) {
            NonStoringLogTailer.LOGGER.info("Waiting for message ended in a timeout.");
            return false;
        } finally {
            this.refreshBarrier();
        }
    }
}
