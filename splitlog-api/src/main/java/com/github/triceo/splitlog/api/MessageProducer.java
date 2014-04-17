package com.github.triceo.splitlog.api;

/**
 * Mark classes that are allowed to send message delivery notifications to
 * others.
 *
 * @param <P>
 *            The type to send out {@link Message} notifications. Typically it
 *            is the implementing type.
 */
public interface MessageProducer<P extends MessageProducer<P>> {

    /**
     * Return the number of consumers that have been
     * {@link #startConsuming(MessageListener)}'d and not
     * {@link #stopConsuming(MessageConsumer)}'d.
     *
     * @return Always >= 0.
     */
    int countConsumers();

    /**
     * Whether or not the particular message consumer is being notified of new
     * messages.
     *
     * @param consumer
     *            Consumer in question.
     * @return True if called between {@link #startConsuming(MessageListener)}
     *         and {@link #stopConsuming(MessageConsumer)}.
     */
    boolean isConsuming(MessageConsumer<P> consumer);

    /**
     * Register a listener to be notified of new messages in this producer.
     *
     * @param listener
     *            Listener in question.
     * @return A newly produced consumer.
     * @throws IllegalArgumentException
     *             When <code>listener instanceof {@link MessageConsumer}</code>
     *             .
     */
    MessageConsumer<P> startConsuming(MessageListener<P> listener);

    /**
     * Tell a consumer to no longer listen to new messages in this producer.
     *
     * @param consumer
     *            Consumer in question.
     * @return True, unless no longer {@link #isConsuming(MessageConsumer)}.
     */
    boolean stopConsuming(MessageConsumer<P> consumer);

}
