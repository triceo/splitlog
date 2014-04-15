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
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageProducer;

final class MessageMetricExchange<T extends Number, S extends MessageProducer<S>> implements MessageConsumer<S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageMetricExchange.class);

    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private MessageMetricCondition<T, S> messageBlockingCondition = null;
    private final Exchanger<Message> messageExchanger = new Exchanger<Message>();

    private final MessageMetric<T, S> metric;

    public MessageMetricExchange(final MessageMetric<T, S> metric) {
        this.metric = metric;
    }

    @Override
    public boolean isStopped() {
        return this.isStopped.get();
    }

    @Override
    public void messageReceived(final Message msg, final MessageDeliveryStatus status, final S source) {
        MessageMetricExchange.LOGGER.info("Notified of message '{}' in state {} from {}.", msg, status, source);
        if (this.messageBlockingCondition == null) {
            MessageMetricExchange.LOGGER.debug("Not blocked.");
            // this does nothing with the message
            return;
        }
        // check if the user code accepts the message
        if (!this.messageBlockingCondition.accept(this.metric)) {
            return;
        }
        MessageMetricExchange.LOGGER
                .debug("Condition passed by message '{}' in state {} from {}.", msg, status, source);
        this.messageBlockingCondition = null;
        try {
            this.messageExchanger.exchange(msg);
        } catch (final InterruptedException e) {
            MessageMetricExchange.LOGGER.warn("Failed to notify Metric of message '{}' in state {} from {}.", msg,
                    status, source, e);
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
    public synchronized Message waitForMessage(final MessageMetricCondition<T, S> condition, final long timeout,
        final TimeUnit unit) {
        this.messageBlockingCondition = condition;
        try {
            MessageMetricExchange.LOGGER.info("Thread blocked waiting for message to pass condition {}.", condition);
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
            MessageMetricExchange.LOGGER.info("Thread unblocked.");
            this.messageBlockingCondition = null;
        }
    }

}
