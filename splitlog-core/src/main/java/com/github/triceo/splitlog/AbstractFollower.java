package com.github.triceo.splitlog;

import java.io.OutputStream;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageFormatter;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.conditions.AllLogWatchMessagesAcceptingCondition;
import com.github.triceo.splitlog.ordering.OriginalOrderingMessageComprator;

/**
 * Internal API for a log follower that, on top of the public API, provides ways
 * for {@link LogWatch} of notifying the follower of new messages. Every
 * follower implementation, such as {@link NonStoringFollower}, needs to extend
 * this class.
 *
 * Will use {@link #getDefaultFormatter()} as default message formatter. Will
 * use {@value #DEFAULT_CONDITION} as a default in getMessages() and write()
 * methods. Will use {@link #DEFAULT_COMPARATOR} as a default order for the
 * messages.
 */
abstract class AbstractFollower<P extends MessageProducer<P>, C extends MessageProducer<C>> implements
        CommonFollower<P, C> {

    private static final MessageComparator DEFAULT_COMPARATOR = OriginalOrderingMessageComprator.INSTANCE;
    private static final SimpleMessageCondition DEFAULT_CONDITION = AllLogWatchMessagesAcceptingCondition.INSTANCE;

    private final MessageExchange<C> exchange = new MessageExchange<C>();

    /**
     * Provide the default formatter for messages in this follower.
     *
     * @return Formatter to use on messages.
     */
    protected abstract MessageFormatter getDefaultFormatter();

    protected MessageExchange<C> getExchange() {
        return this.exchange;
    }

    @Override
    public SortedSet<Message> getMessages() {
        return this.getMessages(AbstractFollower.DEFAULT_COMPARATOR);
    }

    @Override
    public SortedSet<Message> getMessages(final MessageComparator order) {
        return this.getMessages(AbstractFollower.DEFAULT_CONDITION, order);
    }

    @Override
    public SortedSet<Message> getMessages(final SimpleMessageCondition condition) {
        return this.getMessages(condition, AbstractFollower.DEFAULT_COMPARATOR);
    }

    @Override
    public boolean isFollowing() {
        return !this.isStopped();
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MidDeliveryMessageCondition<C> condition) {
        return this.exchange.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MidDeliveryMessageCondition<C> condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.exchange.waitForMessage(condition, timeout, unit);
    }

    @Override
    public boolean write(final OutputStream stream) {
        return this.write(stream, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final MessageComparator order) {
        return this.write(stream, order, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final MessageComparator order, final MessageFormatter formatter) {
        return this.write(stream, AbstractFollower.DEFAULT_CONDITION, order, formatter);
    }

    @Override
    public boolean write(final OutputStream stream, final MessageFormatter formatter) {
        return this.write(stream, AbstractFollower.DEFAULT_CONDITION, formatter);
    }

    @Override
    public boolean write(final OutputStream stream, final SimpleMessageCondition condition) {
        return this.write(stream, condition, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final SimpleMessageCondition condition,
        final MessageComparator order) {
        return this.write(stream, condition, order, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final SimpleMessageCondition condition,
        final MessageFormatter formatter) {
        return this.write(stream, condition, AbstractFollower.DEFAULT_COMPARATOR, formatter);
    }

}
