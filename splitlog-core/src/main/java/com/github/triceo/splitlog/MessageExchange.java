package com.github.triceo.splitlog;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;

final class MessageExchange<S extends MessageProducer<S>> implements MessageConsumer<S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageExchange.class);

    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private MidDeliveryMessageCondition<S> messageBlockingCondition = null;

    private final Exchanger<Message> messageExchanger = new Exchanger<Message>();

    @Override
    public boolean isStopped() {
        return this.isStopped.get();
    }

    @Override
    public void messageReceived(final Message msg, final MessageDeliveryStatus status, final S source) {
        if (this.isStopped()) {
            throw new IllegalStateException("Messsage exchange already stopped.");
        } else if (this.messageBlockingCondition == null) {
            MessageExchange.LOGGER.debug("Not waiting for message '{}' in state {} from {}.", msg, status, source);
            // this does nothing with the message
            return;
        }
        MessageExchange.LOGGER.info("Notified of message '{}' in state {} from {}.", msg, status, source);
        // check if the user code accepts the message
        if (!this.messageBlockingCondition.accept(msg, status, source)) {
            return;
        }
        MessageExchange.LOGGER.debug("Accepted message '{}' in state {} from {}.", msg, status, source);
        this.messageBlockingCondition = null;
        try {
            this.messageExchanger.exchange(msg);
        } catch (final InterruptedException e) {
            MessageExchange.LOGGER.warn("Failed to notify Follower of message '{}' in state {} from {}.", msg, status,
                    source, e);
        }
    }

    @Override
    public boolean stop() {
        return this.isStopped.compareAndSet(false, true);
    }

    /*
     * The method is synchronized, therefore no thread will be able to overwrite
     * an already set condition. That condition will later be unset by the
     * notify*() method calls from the tailing thread.
     */
    public synchronized Message waitForMessage(final MidDeliveryMessageCondition<S> condition, final long timeout,
        final TimeUnit unit) {
        if (this.isStopped()) {
            throw new IllegalStateException("Message exchange already stopped.");
        }
        this.messageBlockingCondition = condition;
        try {
            MessageExchange.LOGGER.info("Thread blocked waiting for message to pass condition {}.", condition);
            if (timeout < 0) {
                return this.messageExchanger.exchange(null);
            } else {
                return this.messageExchanger.exchange(null, timeout, unit);
            }
        } catch (final TimeoutException e) {
            return null;
        } catch (final InterruptedException e) {
            return null;
        } finally { // just in case
            MessageExchange.LOGGER.info("Thread unblocked.");
            this.messageBlockingCondition = null;
        }
    }

}
