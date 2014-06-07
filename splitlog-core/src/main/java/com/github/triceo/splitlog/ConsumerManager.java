package com.github.triceo.splitlog;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.util.LogUtil;
import com.github.triceo.splitlog.util.LogUtil.Level;

class ConsumerManager<P extends MessageProducer<P>> implements MessageProducer<P>, MessageConsumer<P>,
ConsumerRegistrar<P> {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(ConsumerManager.class);

    private final Set<MessageConsumer<P>> consumers = new CopyOnWriteArraySet<MessageConsumer<P>>();
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final BidiMap<String, DefaultMessageMetric<? extends Number, P>> metrics = new DualHashBidiMap<String, DefaultMessageMetric<? extends Number, P>>();

    private final P producer;

    public ConsumerManager(final P producer) {
        this.producer = producer;
    }

    @Override
    public int countConsumers() {
        return this.consumers.size();
    }

    @Override
    public synchronized int countMetrics() {
        return this.metrics.size();
    }

    @Override
    public synchronized MessageMetric<? extends Number, P> getMetric(final String id) {
        return this.metrics.get(id);
    }

    @Override
    public synchronized String getMetricId(final MessageMetric<? extends Number, P> measure) {
        return this.metrics.getKey(measure);
    }

    public P getProducer() {
        return this.producer;
    }

    @Override
    public boolean isConsuming(final MessageConsumer<P> consumer) {
        return this.consumers.contains(consumer);
    }

    @Override
    public synchronized boolean isMeasuring(final MessageMetric<? extends Number, P> metric) {
        return this.metrics.containsValue(metric);
    }

    @Override
    public synchronized boolean isMeasuring(final String id) {
        return this.metrics.containsKey(id);
    }

    @Override
    public boolean isStopped() {
        return this.isStopped.get();
    }

    @Override
    public void messageReceived(final Message message, final MessageDeliveryStatus status, final P producer) {
        if (this.isStopped()) {
            throw new IllegalStateException("Consumer manager already stopped.");
        }
        LogUtil.newMessage(ConsumerManager.LOGGER, Level.INFO, "New message received:", message, status, producer, this);
        for (final MessageConsumer<P> consumer : this.consumers) {
            try {
                consumer.messageReceived(message, status, producer);
            } catch (final Throwable t) {
                // calling user code; we need to be prepared for anything
                ConsumerManager.LOGGER.warn("Failed notifying {} of '{}' with status {}. Stack trace on DEBUG.",
                        consumer, message, status, t.getMessage());
                ConsumerManager.LOGGER.debug("Failed notifying {} of '{}' with status {}.", consumer, message, status,
                        t);
            }
        }
    }

    @Override
    public boolean registerConsumer(final MessageConsumer<P> consumer) {
        if (this.isStopped()) {
            throw new IllegalStateException("Consumer manager already stopped.");
        } else if (this.consumers.add(consumer)) {
            ConsumerManager.LOGGER.info("Registered consumer {} for {}.", consumer, this.producer);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public MessageConsumer<P> startConsuming(final MessageListener<P> listener) {
        if (listener instanceof MessageConsumer<?>) {
            throw new IllegalArgumentException("Cannot consume consumers.");
        }
        final MessageConsumer<P> consumer = new DefaultMessageConsumer<P>(listener, this.producer);
        if (this.registerConsumer(consumer)) {
            ConsumerManager.LOGGER.info("Registered new consumer {} for {}.", consumer, listener);
            return consumer;
        }
        /*
         * we know that there is a consumer with the same properties; disregard
         * the new and return the old instead.
         */
        for (final MessageConsumer<P> existing : this.consumers) {
            if (existing.equals(consumer)) {
                ConsumerManager.LOGGER.info("Retrieve pre-existing consumer {} for {}.", consumer, listener);
                return existing;
            }
        }
        throw new IllegalStateException("Unreachable code.");
    }

    @Override
    public synchronized <T extends Number> MessageMetric<T, P> startMeasuring(final MessageMeasure<T, P> measure,
            final String id) {
        if (this.isStopped()) {
            throw new IllegalStateException("Measuring consumer manager already stopped.");
        } else if (measure == null) {
            throw new IllegalArgumentException("Measure may not be null.");
        } else if (id == null) {
            throw new IllegalArgumentException("ID may not be null.");
        } else if (this.metrics.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ID:" + id);
        }
        ConsumerManager.LOGGER.info("Starting measuring {} in {}.", id, this.getProducer());
        final DefaultMessageMetric<T, P> metric = new DefaultMessageMetric<T, P>(this.getProducer(), measure);
        this.metrics.put(id, metric);
        this.registerConsumer(metric);
        return metric;
    }

    @Override
    public boolean stop() {
        if (!this.isStopped.compareAndSet(false, true)) {
            return false;
        }
        ConsumerManager.LOGGER.info("Stopping consumer manager for {}.", this.producer);
        for (final MessageConsumer<P> consumer : this.consumers) {
            this.stopConsuming(consumer);
        }
        for (final String metricId : new HashSet<String>(this.metrics.keySet())) {
            this.stopMeasuring(metricId);
        }
        ConsumerManager.LOGGER.info("Stopped metrics consumer manager for {}.", this.getProducer());
        return true;
    }

    @Override
    public boolean stopConsuming(final MessageConsumer<P> consumer) {
        if (this.consumers.remove(consumer)) {
            ConsumerManager.LOGGER.info("Unregistered consumer {} for {}.", consumer, this.producer);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public synchronized boolean stopMeasuring(final MessageMetric<? extends Number, P> measure) {
        if (!this.isMeasuring(measure)) {
            return false;
        }
        return this.stopMeasuring(this.metrics.getKey(measure));
    }

    @Override
    public synchronized boolean stopMeasuring(final String id) {
        if (!this.isMeasuring(id)) {
            return false;
        }
        final DefaultMessageMetric<? extends Number, P> removed = this.metrics.remove(id);
        this.stopConsuming(removed);
        ConsumerManager.LOGGER.info("Stopped measuring {} in {}.", id, this.getProducer());
        return true;
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
