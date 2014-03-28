package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageCondition;
import com.github.triceo.splitlog.api.MessageDeliveryCondition;
import com.github.triceo.splitlog.api.MessageDeliveryNotificationSource;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;

/**
 * This is a log follower that holds no message data, just the tags. For message
 * data, it will always turn to the underlying {@link LogWatch}.
 * 
 * This class assumes that LogWatch and user code are the only two threads that
 * use it. Never use one instance of this class from two or more user threads.
 * Otherwise, unpredictable behavior from waitFor() methods is possible.
 */
final class NonStoringFollower extends AbstractLogWatchFollower {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringFollower.class);

    private final MessageExchange exchange = new MessageExchange();

    public NonStoringFollower(final DefaultLogWatch watch) {
        super(watch);
    }

    @Override
    public SortedSet<Message> getMessages(final MessageCondition condition, final MessageComparator order) {
        final SortedSet<Message> messages = new TreeSet<Message>(order);
        messages.addAll(this.getWatch().getAllMessages(this));
        messages.addAll(this.getTags());
        return Collections.unmodifiableSortedSet(messages);
    }

    @Override
    protected void notifyOfAcceptedMessage(final Message msg, final MessageDeliveryNotificationSource source) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.ACCEPTED, source);
        for (final AbstractMergingFollower mf : this.getMergingFollowersToNotify()) {
            mf.notifyOfAcceptedMessage(msg, this);
        }
    }

    @Override
    protected void notifyOfIncomingMessage(final Message msg, final MessageDeliveryNotificationSource source) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.INCOMING, source);
        for (final AbstractMergingFollower mf : this.getMergingFollowersToNotify()) {
            mf.notifyOfIncomingMessage(msg, this);
        }
    }

    @Override
    protected void notifyOfRejectedMessage(final Message msg, final MessageDeliveryNotificationSource source) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.REJECTED, source);
        for (final AbstractMergingFollower mf : this.getMergingFollowersToNotify()) {
            mf.notifyOfRejectedMessage(msg, this);
        }
    }

    @Override
    protected void notifyOfUndeliveredMessage(final Message msg, final MessageDeliveryNotificationSource source) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.UNDELIVERED, source);
        for (final AbstractMergingFollower mf : this.getMergingFollowersToNotify()) {
            mf.notifyOfUndeliveredMessage(msg, this);
        }
    }

    // notifications must be mutually exclusive
    private synchronized void notifyOfMessage(final Message msg, final MessageDeliveryStatus status,
        final MessageDeliveryNotificationSource source) {
        if (source != this.getWatch()) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        NonStoringFollower.LOGGER.info("{} notified of '{}' with status {}.", this, msg, status);
        this.exchange.notifyOfMessage(msg, status, source);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageDeliveryCondition condition) {
        return this.exchange.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
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
        return this.exchange.waitForMessage(condition, timeout, unit);
    }

}