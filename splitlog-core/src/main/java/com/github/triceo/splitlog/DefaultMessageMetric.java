package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageSource;

class DefaultMessageMetric<T extends Number> implements MessageMetric<T> {

    private final MessageMeasure<T> measure;
    private int messageCount;
    private T value;

    public DefaultMessageMetric(final MessageMeasure<T> measure) {
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

    synchronized void notifyOfMessage(final Message msg, final MessageDeliveryStatus status, final MessageSource source) {
        this.value = this.measure.update(this, msg, status, source);
        this.messageCount++;
    }

}
