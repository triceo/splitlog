package com.github.triceo.splitlog;

import java.io.OutputStream;
import java.util.SortedSet;

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageFormatter;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.conditions.AllMessagesAcceptingCondition;
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
abstract class AbstractFollower implements CommonFollower {

    private static final MessageComparator DEFAULT_COMPARATOR = OriginalOrderingMessageComprator.INSTANCE;
    private static final SimpleMessageCondition DEFAULT_CONDITION = AllMessagesAcceptingCondition.INSTANCE;

    @Override
    public SortedSet<Message> getMessages() {
        return this.getMessages(AbstractFollower.DEFAULT_COMPARATOR);
    }

    @Override
    public SortedSet<Message> getMessages(final MessageComparator order) {
        return this.getMessages(AbstractFollower.DEFAULT_CONDITION, order);
    }

    /**
     * Provide the default formatter for messages in this follower.
     *
     * @return Formatter to use on messages.
     */
    protected abstract MessageFormatter getDefaultFormatter();

    @Override
    public boolean write(final OutputStream stream) {
        return this.write(stream, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final SimpleMessageCondition condition) {
        return this.write(stream, condition, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final MessageFormatter formatter) {
        return this.write(stream, AbstractFollower.DEFAULT_CONDITION, formatter);
    }

    @Override
    public SortedSet<Message> getMessages(final SimpleMessageCondition condition) {
        return this.getMessages(condition, AbstractFollower.DEFAULT_COMPARATOR);
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
