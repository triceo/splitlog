package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageCondition;
import com.github.triceo.splitlog.api.MessageDeliveryCondition;
import com.github.triceo.splitlog.api.MessageDeliveryNotificationSource;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;

final class NonStoringMergingFollower extends AbstractMergingFollower {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringFollower.class);

    private final MessageExchange exchange = new MessageExchange();

    public NonStoringMergingFollower(final CommonFollower... followers) {
        super(followers);
    }

    @Override
    public SortedSet<Message> getMessages(final MessageCondition condition, final MessageComparator order) {
        final SortedSet<Message> sorted = new TreeSet<Message>(order);
        for (final Follower f : this.getMerged()) {
            for (final Message m : f.getMessages()) {
                if (!condition.accept(m, f)) {
                    continue;
                }
                sorted.add(m);
            }
        }
        sorted.addAll(this.getTags());
        return Collections.unmodifiableSortedSet(sorted);
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

    @Override
    protected void notifyOfAcceptedMessage(final Message msg, final MessageDeliveryNotificationSource source) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.ACCEPTED, source);
    }

    @Override
    protected void notifyOfIncomingMessage(final Message msg, final MessageDeliveryNotificationSource source) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.INCOMING, source);
    }

    @Override
    protected void notifyOfRejectedMessage(final Message msg, final MessageDeliveryNotificationSource source) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.REJECTED, source);
    }

    @Override
    protected void notifyOfUndeliveredMessage(final Message msg, final MessageDeliveryNotificationSource source) {
        this.notifyOfMessage(msg, MessageDeliveryStatus.UNDELIVERED, source);
    }

    // notifications must be mutually exclusive
    private synchronized void notifyOfMessage(final Message msg, final MessageDeliveryStatus status,
        final MessageDeliveryNotificationSource source) {
        if (!this.getMerged().contains(source)) {
            throw new IllegalArgumentException("Forbidden notification source: " + source);
        }
        NonStoringMergingFollower.LOGGER.info("{} notified of '{}' with status {} by {}.", this, msg, status, source);
        this.exchange.notifyOfMessage(msg, status, source);
    }

}
