package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageProducer;

/**
 * This is an internal API so that the core can register consumers without
 * having to go through
 * {@link #startConsuming(com.github.triceo.splitlog.api.MessageListener)}.
 *
 * @param consumer
 *            Consumer to register.
 */
interface ConsumerRegistrar<P extends MessageProducer<P>> extends MessageProducer<P> {

    /**
     * Consumers registered through here will be available through all the
     * standard methods such as {@link #isConsuming(MessageConsumer)} or
     * {@link #stopConsuming(MessageConsumer)}.
     *
     * @param consumer
     *            Consumer to register.
     */
    void registerConsumer(MessageConsumer<P> consumer);

}
