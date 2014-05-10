package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageProducer;

final class DefaultMessageConsumer<P extends MessageProducer<P>> implements MessageConsumer<P> {

    private final MessageListener<P> listener;
    private final P producer;

    public DefaultMessageConsumer(final MessageListener<P> listener, final P producer) {
        if ((producer == null) || (listener == null)) {
            throw new IllegalArgumentException("Neither producer nor listener may be null.");
        }
        this.producer = producer;
        this.listener = listener;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final DefaultMessageConsumer<P> other = (DefaultMessageConsumer<P>) obj;
        if (this.listener == null) {
            if (other.listener != null) {
                return false;
            }
        } else if (!this.listener.equals(other.listener)) {
            return false;
        }
        if (this.producer == null) {
            if (other.producer != null) {
                return false;
            }
        } else if (!this.producer.equals(other.producer)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.listener == null) ? 0 : this.listener.hashCode());
        result = (prime * result) + ((this.producer == null) ? 0 : this.producer.hashCode());
        return result;
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
