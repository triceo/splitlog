package com.github.triceo.splitlog;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageProducer;

public class ConsumerManager<P extends MessageProducer<P>> implements MessageProducer<P>, MessageConsumer<P> {

    private final Set<MessageConsumer<P>> consumers = new LinkedHashSet<MessageConsumer<P>>();
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final P producer;

    public ConsumerManager(final P producer) {
        this.producer = producer;
    }

    public P getProducer() {
        return this.producer;
    }

    @Override
    public boolean isConsuming(final MessageConsumer<P> consumer) {
        return this.consumers.contains(consumer);
    }

    @Override
    public boolean isStopped() {
        return this.isStopped.get();
    }

    @Override
    public synchronized void
        messageReceived(final Message message, final MessageDeliveryStatus status, final P producer) {
        if (this.isStopped()) {
            throw new IllegalStateException("Consumer manager already stopped.");
        }
        for (final MessageConsumer<P> consumer : this.consumers) {
            consumer.messageReceived(message, status, producer);
        }
    }

    @Override
    public synchronized MessageConsumer<P> startConsuming(final MessageListener<P> listener) {
        if (this.isStopped()) {
            throw new IllegalStateException("Consumer manager already stopped.");
        }
        final MessageConsumer<P> consumer = (listener instanceof MessageConsumer) ? (MessageConsumer<P>) listener
                : new DefaultMessageConsumer<P>(this.producer, listener);
        this.consumers.add(consumer);
        return consumer;
    }

    @Override
    public synchronized boolean stop() {
        if (!this.isStopped.compareAndSet(false, true)) {
            return false;
        }
        for (final MessageConsumer<P> consumer : new LinkedList<MessageConsumer<P>>(this.consumers)) {
            consumer.stop();
        }
        return true;
    }

    @Override
    public synchronized boolean stopConsuming(final MessageConsumer<P> consumer) {
        return this.consumers.remove(consumer);
    }

}
