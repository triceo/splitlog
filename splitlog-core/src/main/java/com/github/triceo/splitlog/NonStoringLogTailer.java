package com.github.triceo.splitlog;

import java.util.Collections;
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
 * Otherwise, unpredictable behavior from waitFor() methods is possible.
 */
class NonStoringLogTailer extends AbstractLogTailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringLogTailer.class);

    private Message receivedMessage;
    private String receivedLine;
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
    public List<Message> getMessages(final MessageCondition condition) {
        final List<Message> messages = new LinkedList<Message>();
        int messageId = 0;
        for (final Message msg : this.getWatch().getAllMessages(this)) {
            // insert a tag if there is one for this particular spot
            if (this.tags.containsKey(messageId)) {
                messages.add(this.tags.get(messageId));
            }
            // insert a message if accepted
            if (condition.accept(msg)) {
                messages.add(msg);
            }
            messageId++;
        }
        // if there is a tag after the last message, this will catch it
        if (this.tags.containsKey(messageId)) {
            messages.add(this.tags.get(messageId));
        }
        return Collections.unmodifiableList(messages);
    }

    @Override
    protected synchronized void notifyOfLine(final String line) {
        if (this.lineBlockingCondition == null) {
            // this does nothing with the line
            return;
        } else if (this.lineBlockingCondition.accept(line)) {
            // we have a line we were looking for, unblock
            try {
                this.blocker.await();
                this.receivedLine = line;
            } catch (final Exception e) {
                this.blocker.reset();
            } finally {
                this.lineBlockingCondition = null;
                this.refreshBarrier();
            }
        }
    }

    @Override
    protected synchronized void notifyOfMessage(final Message msg) {
        if (this.messageBlockingCondition == null) {
            // this does nothing with the message
            return;
        } else if (this.messageBlockingCondition.accept(msg)) {
            // we have a message we were looking for, unblock
            try {
                this.blocker.await();
                this.receivedMessage = msg;
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

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public String waitFor(final LineCondition condition) {
        this.lineBlockingCondition = condition;
        return this.waitForLine(-1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public String waitFor(final LineCondition condition, final long timeout, final TimeUnit unit) {
        if (this.lineBlockingCondition != null) {
            throw new IllegalStateException("This tailer is already waiting for a line.");
        } else if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        this.lineBlockingCondition = condition;
        return this.waitForLine(timeout, unit);
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

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageCondition condition) {
        this.messageBlockingCondition = condition;
        return this.waitForMessage(-1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageCondition condition, final long timeout, final TimeUnit unit) {
        if (this.messageBlockingCondition != null) {
            throw new IllegalStateException("This tailer is already waiting for a message.");
        } else if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        this.messageBlockingCondition = condition;
        return this.waitForMessage(timeout, unit);
    }

    private String waitForLine(final long timeout, final TimeUnit unit) {
        final boolean waitSucceeded = this.waitFor(timeout, unit);
        if (waitSucceeded) {
            final String result = this.receivedLine;
            if (result == null) {
                // should never happen; check concurrency logic if it does
                throw new IllegalStateException("Waiting for a line has succeeded, yet the line is null.");
            } else {
                this.receivedLine = null;
                return result;
            }
        } else {
            return null;
        }
    }

    private Message waitForMessage(final long timeout, final TimeUnit unit) {
        final boolean waitSucceeded = this.waitFor(timeout, unit);
        if (waitSucceeded) {
            final Message result = this.receivedMessage;
            if (result == null) {
                // should never happen; check concurrency logic if it does
                throw new IllegalStateException("Waiting for a message has succeeded, yet the message is null.");
            } else {
                this.receivedMessage = null;
                return result;
            }
        } else {
            return null;
        }
    }
}
