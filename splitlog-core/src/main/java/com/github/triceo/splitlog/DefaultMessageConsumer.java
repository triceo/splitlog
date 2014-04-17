package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageProducer;

final class DefaultMessageConsumer<P extends MessageProducer<P>> implements MessageConsumer<P> {

    private final MessageListener<P> listener;
    private final P producer;

    public DefaultMessageConsumer(final P producer, final MessageListener<P> listener) {
        this.producer = producer;
        this.listener = listener;
    }

    @Override
    public boolean isStopped() {
        return !this.producer.isConsuming(this);
    }

    @Override
    public void messageReceived(final Message message, final MessageDeliveryStatus status, final P producer) {
        this.listener.messageReceived(message, status, producer);
    }

    @Override
    public boolean stop() {
        return this.producer.stopConsuming(this);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("DefaultMessageConsumer [");
        if (this.producer != null) {
            builder.append("producer=").append(this.producer).append(", ");
        }
        if (this.listener != null) {
            builder.append("listener=").append(this.listener);
        }
        builder.append("]");
        return builder.toString();
    }

}
