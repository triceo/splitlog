package com.github.triceo.splitlog;

import java.util.concurrent.TimeUnit;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageProducer;

final class DefaultMessageMetric<T extends Number, S extends MessageProducer<S>> implements MessageMetric<T, S>,
        MessageListener<S> {

    private final MessageMetricExchange<T, S> exchange = new MessageMetricExchange<T, S>(this);
    private final MessageMeasure<T, S> measure;
    private int messageCount;
    private final S source;
    private T value;

    public DefaultMessageMetric(final S source, final MessageMeasure<T, S> measure) {
        if (measure == null) {
            throw new IllegalArgumentException("Measure must not be null.");
        } else if (source == null) {
            throw new IllegalArgumentException("Source must not be null.");
        }
        this.source = source;
        this.measure = measure;
    }

    @Override
    public MessageMeasure<T, S> getMeasure() {
        return this.measure;
    }

    @Override
    public synchronized long getMessageCount() {
        return this.messageCount;
    }

    @Override
    public S getSource() {
        return this.source;
    }

    @Override
    public synchronized T getValue() {
        return this.value;
    }

    @Override
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status, final S source) {
        this.value = this.measure.update(this, msg, status, source);
        this.exchange.messageReceived(msg, status, source);
        this.messageCount++;
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageMetricCondition<T, S> condition) {
        return this.exchange.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MessageMetricCondition<T, S> condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.exchange.waitForMessage(condition, timeout, unit);
    }

}
