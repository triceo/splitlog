package com.github.triceo.splitlog;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageProducer;

public class ConsumerManager<P extends MessageProducer<P>> implements MessageProducer<P>, MessageConsumer<P> {

    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final Set<MessageConsumer<P>> consumers = new LinkedHashSet<MessageConsumer<P>>();

    @Override
    public boolean isConsuming(final MessageConsumer<P> consumer) {
        return this.consumers.contains(consumer);
    }

    @Override
    public boolean isStopped() {
        return this.isStopped.get();
    }

    @Override
    public synchronized void messageReceived(final Message message, final MessageDeliveryStatus status, final P producer) {
        for (final MessageConsumer<P> consumer : this.consumers) {
            consumer.messageReceived(message, status, producer);
        }
    }

    @Override
    public synchronized boolean startConsuming(final MessageConsumer<P> consumer) {
        return this.consumers.add(consumer);
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
