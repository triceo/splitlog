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
     * Whether or not the particular message consumer is being notified of new
     * messages.
     *
     * @param consumer
     *            Consumer in question.
     * @return True if called between {@link #startConsuming(MessageConsumer)}
     *         and {@link #stopConsuming(MessageConsumer)}.
     */
    boolean isConsuming(MessageConsumer<P> consumer);

    /**
     * Register a consumer to be notified of new messages in this producer.
     * 
     * @param consumer
     *            Consumer in question.
     * @return True, unless already {@link #isConsuming(MessageConsumer)}.
     */
    boolean startConsuming(MessageConsumer<P> consumer);

    /**
     * Tell a consumer to no longer listen to new messages in this producer.
     * 
     * @param consumer
     *            Consumer in question.
     * @return True, unless no longer {@link #isConsuming(MessageConsumer)}.
     */
    boolean stopConsuming(MessageConsumer<P> consumer);

}
