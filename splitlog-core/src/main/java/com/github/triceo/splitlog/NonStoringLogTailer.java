package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.BooleanCondition;
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
    private BooleanCondition<Message> messageBlockingCondition = null;
    private BooleanCondition<String> lineBlockingCondition = null;
    // FIXME this assumes that only the tail thread and user thread are present
    private final Semaphore blocker = new Semaphore(1);

    private final Map<Integer, Message> tags = new TreeMap<Integer, Message>();

    public NonStoringLogTailer(final DefaultLogWatch watch) {
        super(watch);
        try {
            // acquire the permit, so that the future waits block
            this.blocker.acquire();
        } catch (final InterruptedException e) {
            throw new IllegalStateException("Couldn't initialize tailer.", e);
        }
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
    protected void notifyOfLine(final String line) {
        if (this.lineBlockingCondition == null) {
            // this does nothing with the line
            return;
        } else if (this.lineBlockingCondition.accept(line)) {
            if (this.blocker.availablePermits() > 0) {
                // this shouldn't happen, since the condition != null
                throw new IllegalStateException("No thread is waiting on the line condition.");
            }
            // we have a line we were looking for, unblock
            this.receivedLine = line;
            this.blocker.release();
        }
    }

    @Override
    protected void notifyOfMessage(final Message msg) {
        if (this.messageBlockingCondition == null) {
            // this does nothing with the message
            return;
        } else if (this.messageBlockingCondition.accept(msg)) {
            if (this.blocker.availablePermits() > 0) {
                // this shouldn't happen, since the condition != null
                throw new IllegalStateException("No thread is waiting on the message condition.");
            }
            // we have a message we were looking for, unblock
            this.receivedMessage = msg;
            this.blocker.release();
        }
    }

    private synchronized void setLineCondition(final BooleanCondition<String> condition) {
        if (this.lineBlockingCondition != null) {
            throw new IllegalStateException("Another thread is already waiting for a line.");
        }
        this.lineBlockingCondition = condition;
    }

    private synchronized void setMessageCondition(final BooleanCondition<Message> condition) {
        if (this.messageBlockingCondition != null) {
            throw new IllegalStateException("Another thread is already waiting for a message.");
        }
        this.messageBlockingCondition = condition;
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
        return this.waitForLine(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public String waitFor(final LineCondition condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.waitForLine(condition, timeout, unit);
    }

    private synchronized boolean waitFor(final long timeout, final TimeUnit unit) {
        try {
            if (timeout < 0) {
                this.blocker.acquire();
                return true;
            } else {
                return this.blocker.tryAcquire(timeout, unit);
            }
        } catch (final InterruptedException e) {
            NonStoringLogTailer.LOGGER.warn("Waiting interrupted.", e);
            return false;
        }
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageCondition condition) {
        return this.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageCondition condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.waitForMessage(condition, timeout, unit);
    }

    private String waitForLine(final BooleanCondition<String> condition, final long timeout, final TimeUnit unit) {
        this.setLineCondition(condition);
        final boolean waitSucceeded = this.waitFor(timeout, unit);
        if (waitSucceeded) {
            final String result = this.receivedLine;
            if (result == null) {
                // should never happen; check concurrency logic if it does
                throw new IllegalStateException("Waiting for a line has succeeded, yet the line is null.");
            } else {
                this.receivedLine = null;
                this.lineBlockingCondition = null;
                return result;
            }
        } else {
            return null;
        }
    }

    private Message waitForMessage(final BooleanCondition<Message> condition, final long timeout, final TimeUnit unit) {
        this.setMessageCondition(condition);
        final boolean waitSucceeded = this.waitFor(timeout, unit);
        if (waitSucceeded) {
            final Message result = this.receivedMessage;
            if (result == null) {
                // should never happen; check concurrency logic if it does
                throw new IllegalStateException("Waiting for a message has succeeded, yet the message is null.");
            } else {
                this.receivedMessage = null;
                this.messageBlockingCondition = null;
                return result;
            }
        } else {
            return null;
        }
    }
}
