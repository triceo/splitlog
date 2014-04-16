package com.github.triceo.splitlog.api;

import java.io.OutputStream;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

/**
 * Follower's primary function is to allow users to work with their portion of
 * the tailed log file. It provides means for a blocking wait for particular
 * chunks, and can also send these chunks to output.
 *
 * Messages get into the follower when {@link LogWatch} notifies it of them.
 * Alternatively, each {@link Follower#tag(String)} will create a Message within
 * the follower and not notify anyone.
 */
public interface CommonFollower<P extends MessageProducer<P>, C extends MessageProducer<C>> extends MessageConsumer<C> {

    /**
     * Retrieve messages that this follower has been notified of, and tags. They
     * will appear in the order in which we have been notified of them.
     *
     * @return Messages we have been notified of, and tags.
     */
    SortedSet<Message> getMessages();

    /**
     * Retrieve messages that this follower has been notified of, and tags, in a
     * given order.
     *
     * @param order
     *            The comparator that will be used to order the messages.
     * @return Messages we have been notified of, and tags.
     */
    SortedSet<Message> getMessages(MessageComparator order);

    /**
     * Retrieve messages that this follower has been notified of, if a certain
     * condition holds true for them, and tags. They will be in the order given.
     *
     * @param condition
     *            The condition.
     * @return Messages we have been notified of, for which the condition holds
     *         true, and tags.
     */
    SortedSet<Message> getMessages(final SimpleMessageCondition condition);

    /**
     * Retrieve messages that this follower has been notified of, if a certain
     * condition holds true for them, and tags. They will be in the order in
     * which we have been notified of them.
     *
     * @param condition
     *            The condition.
     * @param order
     *            The comparator that will be used to order the messages.
     * @return Messages we have been notified of, for which the condition holds
     *         true, and tags.
     */
    SortedSet<Message> getMessages(final SimpleMessageCondition condition, final MessageComparator order);

    /**
     * Use {@link #isStopped()} instead.
     *
     * Whether or not this follower is still capable of receiving messages. It
     * is suggested that the reference to this follower be thrown away
     * immediately after the user has processed the results of
     * {@link #getMessages()} or {@link #getMessages(SimpleMessageCondition)}.
     * {@link LogWatch} may then be able to free the memory occupied by those
     * messages.
     *
     * @return True if following.
     */
    @Deprecated
    boolean isFollowing();

    /**
     * Merge this {@link CommonFollower} with another. This
     * {@link CommonFollower} has a responsibility of notifying the resulting
     * {@link MergingFollower} of every {@link Message} that it receives, until
     * such time that {@link MergingFollower#separate(Follower)} is called on
     * it.
     *
     * @param f
     *            To merge with.
     * @return A new {@link MergingFollower}, that will merge both
     *         {@link CommonFollower}s. If any of the {@link CommonFollower}s
     *         already is a {@link MergingFollower}, the returned instance will
     *         hold every merged {@link CommonFollower} individually and not
     *         compose {@link MergingFollower}s.
     */
    MergingFollower mergeWith(Follower f);

    /**
     * Merge this {@link CommonFollower} with another. This
     * {@link CommonFollower} has a responsibility of notifying the resulting
     * {@link MergingFollower} of every {@link Message} that it receives, until
     * such time that {@link MergingFollower#separate(Follower)} is called on
     * it.
     *
     * @param f
     *            To merge with.
     * @return A new {@link MergingFollower}, that will merge both
     *         {@link CommonFollower}s. If any of the {@link CommonFollower}s
     *         already is a {@link MergingFollower}, the returned instance will
     *         hold every merged {@link CommonFollower} individually and not
     *         compose {@link MergingFollower}s.
     */
    MergingFollower mergeWith(MergingFollower f);

    /**
     * Will block until a message arrives, for which the condition is true.
     *
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Null if the method unblocked due to some other reason.
     * @throws IllegalStateException
     *             When already {@link #isStopped()}.
     */
    Message waitFor(MidDeliveryMessageCondition<C> condition);

    /**
     * Will block until a message arrives, for which the condition is true. If
     * none arrives before the timeout, it unblocks anyway.
     *
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @param timeout
     *            Time before forcibly aborting.
     * @param unit
     *            Unit of time.
     * @return Null if the method unblocked due to some other reason.
     * @throws IllegalStateException
     *             When already {@link #isStopped()}.
     */
    Message waitFor(MidDeliveryMessageCondition<C> condition, long timeout, TimeUnit unit);

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
     * Will write to a stream the result of
     * {@link #getMessages(MessageComparator)}, using a {@link MessageFormatter}
     * implementation of its own choosing. Will close the stream.
     *
     * @param stream
     *            Target.
     * @param order
     *            The comparator to pass to
     *            {@link #getMessages(MessageComparator)}.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final MessageComparator order);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(MessageComparator)}, using given
     * {@link MessageFormatter}. Will close the stream.
     *
     * @param stream
     *            Target.
     * @param order
     *            The comparator to pass to
     *            {@link #getMessages(MessageComparator)}.
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
     * {@link #getMessages(SimpleMessageCondition)}, using a
     * {@link MessageFormatter} implementation of its own choosing. Will close
     * the stream.
     *
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(SimpleMessageCondition)}.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final SimpleMessageCondition condition);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(SimpleMessageCondition, MessageComparator)}, using a
     * {@link MessageFormatter} implementation of its own choosing. Will close
     * the stream.
     *
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(SimpleMessageCondition, MessageComparator)}
     *            .
     * @param order
     *            The comparator to pass to
     *            {@link #getMessages(SimpleMessageCondition, MessageComparator)}
     *            .
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final SimpleMessageCondition condition, final MessageComparator order);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(SimpleMessageCondition, MessageComparator)}, using
     * given {@link MessageFormatter}. Will close the stream.
     *
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(SimpleMessageCondition, MessageComparator)}
     *            .
     * @param order
     *            The comparator to pass to
     *            {@link #getMessages(SimpleMessageCondition, MessageComparator)}
     *            .
     * @param formatter
     *            Formatter to use to transform message into string.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final SimpleMessageCondition condition, final MessageComparator order,
        final MessageFormatter formatter);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(SimpleMessageCondition)}, using given
     * {@link MessageFormatter}. Will close the stream.
     *
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(SimpleMessageCondition)}.
     * @param formatter
     *            Formatter to use to transform message into string.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final SimpleMessageCondition condition, final MessageFormatter formatter);

}
