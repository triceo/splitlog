package com.github.triceo.splitlog.api;

/**
 * Implementors of this interface state that they are interested in knowing when
 * a new {@link Message} appears in the log.
 *
 * Users shouldn't implement this interface. Instead, they should implement
 * {@link MessageListener} and obtain their {@link MessageConsumer}s from
 * {@link MessageProducer}s.
 *
 * @param <P>
 *            The source that they expect such notifications from.
 */
public interface MessageConsumer<P extends MessageProducer<P>> extends MessageListener<P> {

    /**
     * Whether or not {@link #stop()} has been called.
     *
     * @return True if called.
     */
    boolean isStopped();

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
