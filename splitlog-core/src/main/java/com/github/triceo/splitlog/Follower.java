package com.github.triceo.splitlog;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;

/**
 * Follower's primary function is to allow users to work with their portion of
 * the tailed log file. It provides means for a blocking wait for particular
 * chunks, and can also send these chunks to output.
 * 
 */
public interface Follower {

    /**
     * Retrieve messages that this follower has been notified of, including tags
     * 
     * @return Messages we are aware of, in their original order.
     */
    List<Message> getMessages();

    /**
     * Retrieve messages that this follower has been notified of, if a certain
     * condition holds true for them, including tags.
     * 
     * @param condition
     *            The condition.
     * @return Every message we are aware of, for which the condition holds
     *         true.
     */
    List<Message> getMessages(final MessageCondition condition);

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
     * Will write to a stream the result of {@link #getMessages()}. Will close
     * the stream.
     * 
     * @param stream
     *            Target.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream);

    /**
     * Will write to a stream the result of
     * {@link #getMessages(MessageCondition)}. Will close the stream.
     * 
     * @param stream
     *            Target.
     * @param condition
     *            The condition to pass to
     *            {@link #getMessages(MessageCondition)}.
     * @return True if written, false otherwise.
     */
    boolean write(final OutputStream stream, final MessageCondition condition);
}
