package com.github.triceo.splitlog;

import java.util.concurrent.TimeUnit;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageSource;

final class DefaultMessageMetric<T extends Number> implements MessageMetric<T>, MessageListener<MessageSource> {

    private final MessageMetricExchange<T> exchange = new MessageMetricExchange<T>(this);
    private final MessageMeasure<T> measure;
    private int messageCount;
    private T value;

    public DefaultMessageMetric(final MessageMeasure<T> measure) {
        if (measure == null) {
            throw new IllegalArgumentException("Measure must not be null.");
        }
        this.measure = measure;
    }

    @Override
    public MessageMeasure<T> getMeasure() {
        return this.measure;
    }

    @Override
    public synchronized long getMessageCount() {
        return this.messageCount;
    }

    @Override
    public synchronized T getValue() {
        return this.value;
    }

    @Override
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status,
        final MessageSource source) {
        this.value = this.measure.update(this, msg, status, source);
        this.exchange.messageReceived(msg, status, source);
        this.messageCount++;
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageMetricCondition<T> condition) {
        return this.exchange.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageMetricCondition<T> condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.exchange.waitForMessage(condition, timeout, unit);
    }

}
