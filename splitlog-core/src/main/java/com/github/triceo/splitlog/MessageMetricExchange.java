package com.github.triceo.splitlog;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageSource;

final class MessageMetricExchange<T extends Number> implements MessageListener<MessageSource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageMetricExchange.class);

    private MessageMetricCondition<T> messageBlockingCondition = null;
    private final Exchanger<Message> messageExchanger = new Exchanger<Message>();
    private final MessageMetric<T> metric;

    public MessageMetricExchange(final MessageMetric<T> metric) {
        this.metric = metric;
    }

    @Override
    public void messageReceived(final Message msg, final MessageDeliveryStatus status, final MessageSource source) {
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

    /*
     * The method is synchronized, therefore no thread will be able to overwrite
     * an already set condition. That condition will later be unset by the
     * notify*() method calls from the tailing thread.
     */
    public synchronized Message waitForMessage(final MessageMetricCondition<T> condition, final long timeout,
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
