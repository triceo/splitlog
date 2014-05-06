package com.github.triceo.splitlog.exchanges;

import java.util.concurrent.Callable;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

abstract class AbstractMessageExchange<C, S extends MessageProducer<S>> implements MessageConsumer<S>,
        Callable<Message> {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(AbstractMessageExchange.class);

    private final C blockingCondition;
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final Exchanger<Message> messageExchanger = new Exchanger<Message>();

    protected AbstractMessageExchange(final C condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Must provide condition.");
        }
        this.blockingCondition = condition;
    }

    @Override
    public Message call() {
        if (this.isStopped()) {
            throw new IllegalStateException("Message exchange already stopped.");
        }
        try {
            AbstractMessageExchange.LOGGER.info("Thread blocked waiting for message to pass condition {}.",
                    this.getBlockingCondition());
            return this.messageExchanger.exchange(null);
        } catch (final InterruptedException e) {
            return null;
        } finally { // just in case
            AbstractMessageExchange.LOGGER.info("Thread unblocked.");
        }
    }

    protected C getBlockingCondition() {
        return this.blockingCondition;
    }

    protected abstract boolean isAccepted(final Message msg, final MessageDeliveryStatus status, final S source);

    @Override
    public boolean isStopped() {
        return this.isStopped.get();
    }

    @Override
    public void messageReceived(final Message msg, final MessageDeliveryStatus status, final S source) {
        if (this.isStopped()) {
            throw new IllegalStateException("Message exchange already stopped.");
        }
        AbstractMessageExchange.LOGGER.info("Notified of message '{}' in state {} from {}.", msg, status, source);
        // check if the user code accepts the message
        if (!this.isAccepted(msg, status, source)) {
            return;
        }
        AbstractMessageExchange.LOGGER.debug("Condition passed by message '{}' in state {} from {}.", msg, status,
                source);
        try {
            this.messageExchanger.exchange(msg);
        } catch (final InterruptedException e) {
            AbstractMessageExchange.LOGGER.warn("Failed to notify of message '{}' in state {} from {}.", msg, status,
                    source, e);
        }
    }

    @Override
    public boolean stop() {
        return this.isStopped.compareAndSet(false, true);
    }

}
