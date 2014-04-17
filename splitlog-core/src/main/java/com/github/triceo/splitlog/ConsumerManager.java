package com.github.triceo.splitlog;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageProducer;

class ConsumerManager<P extends MessageProducer<P>> implements MessageProducer<P>, MessageConsumer<P>,
ConsumerRegistrar<P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerManager.class);

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
            ConsumerManager.LOGGER.debug("{} notified of '{}' with status {}.", consumer, message, status);
        }
    }

    @Override
    public synchronized void registerConsumer(final MessageConsumer<P> consumer) {
        this.consumers.add(consumer);
        ConsumerManager.LOGGER.info("Registered consumer {} for {}.", consumer, this.producer);
    }

    @Override
    public synchronized MessageConsumer<P> startConsuming(final MessageListener<P> listener) {
        if (this.isStopped()) {
            throw new IllegalStateException("Consumer manager already stopped.");
        } else if (listener instanceof MessageConsumer<?>) {
            throw new IllegalArgumentException("Cannot consume consumers.");
        }
        final MessageConsumer<P> consumer = new DefaultMessageConsumer<P>(this.producer, listener);
        this.consumers.add(consumer);
        ConsumerManager.LOGGER.info("Started consumer {} for {}.", consumer, this.producer);
        return consumer;
    }

    @Override
    public synchronized boolean stop() {
        if (!this.isStopped.compareAndSet(false, true)) {
            return false;
        }
        ConsumerManager.LOGGER.info("Stopping consumer manager for {}.", this.producer);
        for (final MessageConsumer<P> consumer : new LinkedList<MessageConsumer<P>>(this.consumers)) {
            consumer.stop();
        }
        ConsumerManager.LOGGER.info("Stopped consumer manager for {}.", this.producer);
        return true;
    }

    @Override
    public synchronized boolean stopConsuming(final MessageConsumer<P> consumer) {
        if (this.consumers.remove(consumer)) {
            ConsumerManager.LOGGER.info("Stopped consumer {} for {}.", consumer, this.producer);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ConsumerManager [");
        if (this.getProducer() != null) {
            builder.append("getProducer()=").append(this.getProducer()).append(", ");
        }
        builder.append("isStopped()=").append(this.isStopped()).append("]");
        return builder.toString();
    }

}
