package com.github.triceo.splitlog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.io.IOUtils;

import com.github.triceo.splitlog.conditions.AllMessagesAcceptingCondition;
import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.formatters.MessageFormatter;
import com.github.triceo.splitlog.ordering.MessageComparator;
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
    private static final MessageCondition DEFAULT_CONDITION = AllMessagesAcceptingCondition.INSTANCE;

    private final Set<Message> tags = new LinkedHashSet<Message>();

    @Override
    public SortedSet<Message> getMessages() {
        return this.getMessages(AbstractFollower.DEFAULT_COMPARATOR);
    }

    @Override
    public SortedSet<Message> getMessages(final MessageComparator order) {
        return this.getMessages(AbstractFollower.DEFAULT_CONDITION, order);
    }

    @Override
    public Message tag(final String tagLine) {
        final Message message = new MessageBuilder(tagLine).buildTag();
        this.tags.add(message);
        return message;
    }

    protected Set<Message> getTags() {
        return Collections.unmodifiableSet(this.tags);
    }

    /**
     * Notify the follower that it has been terminated before a message could be
     * delivered completely. Must never be called by users, just from the
     * library code.
     * 
     * @param msg
     *            The message.
     * @param source
     *            Where does the notification come from.
     * @throws IllegalArgumentException
     *             In case the source is a class that should not access to this.
     */
    protected abstract void notifyOfUndeliveredMessage(Message msg, MessageDeliveryNotificationSource source);

    /**
     * Notify the follower of a new line in the watched log. Must never be
     * called by users, just from the library code.
     * 
     * @param msg
     *            The message.
     * @param source
     *            Where does the notification come from.
     * @throws IllegalArgumentException
     *             In case the source is a class that should not access to this.
     */
    protected abstract void notifyOfIncomingMessage(Message msg, MessageDeliveryNotificationSource source);

    /**
     * Notify the follower of a new message in the watched log. Must never be
     * called by users, just from the library code.
     * 
     * @param msg
     *            The message.
     * @param source
     *            Where does the notification come from.
     * @throws IllegalArgumentException
     *             In case the source is a class that should not access to this.
     */
    protected abstract void notifyOfAcceptedMessage(Message msg, MessageDeliveryNotificationSource source);

    /**
     * Notify the follower of a new message from the log that was rejected from
     * entering the log watch. Must never be called by users, just from the
     * library code.
     * 
     * @param msg
     *            The message.
     * @param source
     *            Where does the notification come from.
     * @throws IllegalArgumentException
     *             In case the source is a class that should not access to this.
     */
    protected abstract void notifyOfRejectedMessage(Message msg, MessageDeliveryNotificationSource source);

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
    public boolean write(final OutputStream stream, final MessageCondition condition) {
        return this.write(stream, condition, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final MessageFormatter formatter) {
        return this.write(stream, AbstractFollower.DEFAULT_CONDITION, formatter);
    }

    @Override
    public boolean write(final OutputStream stream, final MessageCondition condition, final MessageComparator order,
        final MessageFormatter formatter) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream may not be null.");
        } else if (condition == null) {
            throw new IllegalArgumentException("Condition may not be null.");
        } else if (order == null) {
            throw new IllegalArgumentException("Comparator may not be null.");
        }
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            for (final Message msg : this.getMessages(condition, order)) {
                w.write(formatter.format(msg));
                w.newLine();
            }
            return true;
        } catch (final IOException ex) {
            return false;
        } finally {
            IOUtils.closeQuietly(w);
        }
    }

    @Override
    public SortedSet<Message> getMessages(final MessageCondition condition) {
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
    public boolean write(final OutputStream stream, final MessageCondition condition, final MessageComparator order) {
        return this.write(stream, condition, order, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final MessageCondition condition, final MessageFormatter formatter) {
        return this.write(stream, condition, AbstractFollower.DEFAULT_COMPARATOR, formatter);
    }

}
