package com.github.triceo.splitlog;

import java.io.OutputStream;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;
import com.github.triceo.splitlog.formatters.MessageFormatter;
import com.github.triceo.splitlog.ordering.MessageComparator;

/**
 * Follower's primary function is to allow users to work with their portion of
 * the tailed log file. It provides means for a blocking wait for particular
 * chunks, and can also send these chunks to output.
 * 
 * Messages get into the follower when {@link LogWatch} notifies it of them.
 */
public interface CommonFollower {

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
    SortedSet<Message> getMessages(MessageComparator order);

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
    SortedSet<Message> getMessages(final MessageCondition condition, final MessageComparator order);

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
     * Mark the current location in the tail by a custom message.
     * 
     * In case the messages before and after the tag should be discarded in the
     * future, the tag should still remain in place - this will give users the
     * notification that some messages had been discarded.
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
    boolean write(final OutputStream stream, final MessageComparator order);

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
    boolean write(final OutputStream stream, final MessageComparator order, final MessageFormatter formatter);

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
    boolean write(final OutputStream stream, final MessageCondition condition, final MessageComparator order);

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
    boolean write(final OutputStream stream, final MessageCondition condition, final MessageComparator order,
        final MessageFormatter formatter);

    /**
     * Merge this {@link CommonFollower} with another. This
     * {@link CommonFollower} has a responsibility of notifying the resulting
     * {@link MergingFollower} of every {@link Message} that it receives, until
     * such time that {@link MergingFollower#separate(CommonFollower)} is called
     * on it.
     * 
     * @param f
     *            To merge with.
     * @return A new {@link MergingFollower}, that will merge both
     *         {@link CommonFollower}s. If any of the {@link CommonFollower}s
     *         already is a {@link MergingFollower}, the returned instance will
     *         hold every merged {@link CommonFollower} individually and not
     *         compose {@link MergingFollower}s.
     */
    MergingFollower mergeWith(CommonFollower f);

}
