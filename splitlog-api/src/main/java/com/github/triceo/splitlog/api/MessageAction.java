package com.github.triceo.splitlog.api;

/**
 * Allows to specify an action to be executed on top of a particular
 * {@link Message}.
 *
 * @param <S>
 *            Where the message comes from.
 */
public interface MessageAction<S extends MessageProducer<S>> {

    /**
     * Execute the action, typically on background. Exceptions thrown here will
     * be swallowed and, at the very best, logged.
     *
     * @param message
     *            Message that triggers the action.
     * @param source
     *            The source of the message.
     * @return Whatever the operation wants to return, null on failure.
     */
    void execute(final Message message, final S source);

}
