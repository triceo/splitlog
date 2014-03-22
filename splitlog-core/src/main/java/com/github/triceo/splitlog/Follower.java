package com.github.triceo.splitlog;

import java.io.OutputStream;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;
import com.github.triceo.splitlog.formatters.MessageFormatter;

/**
 * Follower's primary function is to allow users to work with their portion of
 * the tailed log file. It provides means for a blocking wait for particular
 * chunks, and can also send these chunks to output.
 * 
 * Messages get into the Follower when {@link LogWatch} notifies it of them.
 */
public interface Follower {

    /**
     * Retrieve messages that this follower has been notified of, including
     * tags. They will appear in the order in which we have been notified of
     * them.
     * 
     * @return Messages we have been notified of.
     */
    SortedSet<Message> getMessages();

    /**
     * Retrieve messages that this follower has been notified of, including
     * tags, in a given order.
     * 
     * @param order
     *            The comparator that will be used to order the messages.
     * @return Messages we have been notified of.
     */
    SortedSet<Message> getMessages(Comparator<Message> order);

    /**
     * Retrieve messages that this follower has been notified of, if a certain
     * condition holds true for them, including tags. They will be in the order
     * in which we have been notified of them.
     * 
     * @param condition
     *            The condition.
     * @param order
     *            The comparator that will be used to order the messages.
     * @return Messages we have been notified of, for which the condition holds
     *         true.
     */
    SortedSet<Message> getMessages(final MessageCondition condition, final Comparator<Message> order);

    /**
     * Retrieve messages that this follower has been notified of, if a certain
     * condition holds true for them, including tags. They will be in the order
     * given.
     * 
     * @param condition
     *            The condition.
     * @return Messages we have been notified of, for which the condition holds
     *         true.
     */
    SortedSet<Message> getMessages(final MessageCondition condition);

    /**
     * Whether or not this follower is still actively following its
     * {@link LogWatch}. It is suggested that the reference to this follower be
     * thrown away immediately after the user has processed the results of
     * {@link #getMessages()} or {@link #getMessages(MessageCondition)}.
     * {@link LogWatch} may then be able to free the memory occupied by those
     * messages.
     * 
     * @return True if following.
     */
    boolean isFollowing();

    /**
     * Mark the current location in the tail by a custom message. It is up to
     * the implementors to decide whether or not a tag inserted at the same time
     * twice is inserted twice, or the second tag overwrites the first.
     * 
     * @param tagLine
     *            Text of the message.
     * @return The tag message that was recorded.
     */
    Message tag(String tagLine);

    /**
     * Will block until a message arrives, for which the condition is true.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Null if the method unblocked due to some other reason.
     */
    Message waitFor(MessageDeliveryCondition condition);

    /**
     * Will block until a message arrives, for which the condition is true. If
     * none arrives before the timeout, it unblocks anyway.
     * 
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Null if the method unblocked due to some other reason.
     */
    Message waitFor(MessageDeliveryCondition condition, long timeout, TimeUnit unit);

    /**
     * Will write to a stream the result of {@link #getMessages()}, using a
     * {@link MessageFormatter} implementation of its own choosing. Will close
     * the stream.
     * 
     * @param stream
     *            Target.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream);

    /**
     * Will write to a stream the result of {@link #getMessages(Comparator)},
     * using a {@link MessageFormatter} implementation of its own choosing. Will
     * close the stream.
     * 
     * @param stream
     *            Target.
     * @param order
     *            The comparator to pass to {@link #getMessages(Comparator)}.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final Comparator<Message> order);

    /**
     * Will write to a stream the result of {@link #getMessages(Comparator)},
     * using given {@link MessageFormatter}. Will close the stream.
     * 
     * @param stream
     *            Target.
     * @param order
     *            The comparator to pass to {@link #getMessages(Comparator)}.
     * @param formatter
     *            Formatter to use to transform message into string.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final Comparator<Message> order, final MessageFormatter formatter);

    /**
     * Will write to a stream the result of {@link #getMessages()}, using given
     * {@link MessageFormatter}. Will close the stream.
     * 
     * @param stream
     *            Target.
     * @param formatter
     *            Formatter to use to transform message into string.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final MessageFormatter formatter);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(MessageCondition)}, using a {@link MessageFormatter}
     * implementation of its own choosing. Will close the stream.
     * 
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(MessageCondition)}.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final MessageCondition condition);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(MessageCondition, Comparator)}, using a
     * {@link MessageFormatter} implementation of its own choosing. Will close
     * the stream.
     * 
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(MessageCondition, Comparator)}.
     * @param order
     *            The comparator to pass to
     *            {@link #getMessages(MessageCondition, Comparator)}.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final MessageCondition condition, final Comparator<Message> order);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(MessageCondition)}, using given
     * {@link MessageFormatter}. Will close the stream.
     * 
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(MessageCondition)}.
     * @param formatter
     *            Formatter to use to transform message into string.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final MessageCondition condition, final MessageFormatter formatter);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(MessageCondition, Comparator)}, using given
     * {@link MessageFormatter}. Will close the stream.
     * 
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(MessageCondition, Comparator)}.
     * @param order
     *            The comparator to pass to
     *            {@link #getMessages(MessageCondition, Comparator)}.
     * @param formatter
     *            Formatter to use to transform message into string.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final MessageCondition condition, final Comparator<Message> order,
        final MessageFormatter formatter);
}
