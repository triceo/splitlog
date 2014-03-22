package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;

/**
 * This is a log follower that holds no message data, just the tags. For message
 * data, it will always turn to the underlying {@link LogWatch}.
 * 
 * This class assumes that LogWatch and user code are the only two threads that
 * use it. Never use one instance of this class from two or more user threads.
 * Otherwise, unpredictable behavior from waitFor() methods is possible.
 */
final class NonStoringFollower extends AbstractFollower {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringFollower.class);

    private MessageDeliveryCondition messageBlockingCondition = null;
    private final Exchanger<Message> messageExchanger = new Exchanger<Message>();
    private final Map<Integer, Message> tags = new TreeMap<Integer, Message>();

    public NonStoringFollower(final DefaultLogWatch watch) {
        super(watch);
    }

    @Override
    public SortedSet<Message> getMessages(final MessageCondition condition, final Comparator<Message> order) {
        final SortedSet<Message> messages = new TreeSet<Message>(order);
        /*
         * get the last message ID; done as close as possible to retrieving the
         * list of messages, so that it would be unlikely the list would be
         * modified inbetween, therefore causing the ID to no longer be valid.
         */
        int maxMessageId = 0;
        // and now add the tags for which we still have the messages
        for (final SortedMap.Entry<Integer, Message> entry : this.getWatch().getAllMessages(this).entrySet()) {
            final int messageId = entry.getKey();
            final Message msg = entry.getValue();
            // insert a tag if there is one for this particular spot
            if (this.tags.containsKey(messageId)) {
                messages.add(this.tags.get(messageId));
            }
            // insert a message if accepted
            if (condition.accept(msg)) {
                messages.add(msg);
            }
            maxMessageId = messageId + 1;
        }
        // if there is a tag after the last message, this will catch it
        if (this.tags.containsKey(maxMessageId)) {
            messages.add(this.tags.get(maxMessageId));
        }
        return Collections.unmodifiableSortedSet(messages);
    }

    @Override
    protected void notifyOfAcceptedMessage(final Message msg) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.ACCEPTED);
    }

    @Override
    protected void notifyOfIncomingMessage(final Message msg) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.INCOMING);
    }

    private void notifyOfMessage(final Message msg, final MessageDeliveryStatus status) {
        if (this.messageBlockingCondition == null) {
            // this does nothing with the message
            return;
        }
        if (!this.messageBlockingCondition.accept(msg, status)) {
            return;
        }
        this.messageBlockingCondition = null;
        try {
            this.messageExchanger.exchange(msg);
        } catch (final InterruptedException e) {
            NonStoringFollower.LOGGER.warn("Notifying follower {} of message {} in state {} failed.", this, msg,
                    status, e);
        }
    }

    @Override
    protected void notifyOfRejectedMessage(final Message msg) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.REJECTED);
    }

    @Override
    protected void notifyOfUndeliveredMessage(final Message msg) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.UNDELIVERED);
    }

    /**
     * See {@link AbstractFollower#tag(String)}. Subsequent tags at the same
     * locations will overwrite each other.
     */
    @Override
    public synchronized Message tag(final String tagLine) {
        final int messageId = this.getWatch().getEndingMessageId(this) + 1;
        final Message message = new Message(tagLine);
        this.tags.put(messageId, message);
        return message;
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageDeliveryCondition condition) {
        return this.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageDeliveryCondition condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.waitForMessage(condition, timeout, unit);
    }

    /*
     * The method is synchronized, therefore no thread will be able to overwrite
     * an already set condition. That condition will later be unset by the
     * notify*() method calls from the tailing thread.
     */
    private synchronized Message waitForMessage(final MessageDeliveryCondition condition, final long timeout,
        final TimeUnit unit) {
        this.messageBlockingCondition = condition;
        try {
            if (timeout < 0) {
                return this.messageExchanger.exchange(null);
            } else {
                return this.messageExchanger.exchange(null, timeout, unit);
            }
        } catch (final TimeoutException e) {
            return null;
        } catch (final InterruptedException e) {
            return null;
        } finally { // just in case
            this.messageBlockingCondition = null;
        }
    }

}