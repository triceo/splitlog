package com.github.triceo.splitlog.api;

/**
 * Implementors of this interface state that they are interested in knowing when
 * a new {@link Message} appears in the log.
 *
 * @param <P>
 *            The source that they expect such notifications from.
 */
public interface MessageConsumer<P extends MessageProducer<P>> {

    /**
     * Whether or not {@link #stop()} has been called.
     *
     * @return True if called.
     */
    boolean isStopped();

    /**
     * Notify the code of a new message becoming available in the log.
     *
     * Although this method is public, it only serves as means of communication
     * between Splitlog internals. It is forbidden for user code to ever call
     * this method. Only the authorized sources, all part of Splitlog core, may
     * use it.
     *
     * Implementors are encouraged to properly synchronize this method, as it is
     * expected that message notifications will be received in the same order in
     * which the messages appear in the log.
     *
     * @param message
     *            Message in question.
     * @param status
     *            Current status of the message.
     * @param producer
     *            The code that is notifying us of this event.
     * @throws IllegalStateException
     *             When already {@link #isStopped()}.
     */
    void messageReceived(Message message, MessageDeliveryStatus status, P producer);

    /**
     * This consumer will no longer receive any messages.
     *
     * It is the duty of implementors to make sure that {@link MessageProducer}
     * do not send any more {@link Message}s here.
     *
     * @return False if {@link #isStopped()}.
     */
    boolean stop();

}
