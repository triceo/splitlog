package com.github.triceo.splitlog;

import java.util.HashSet;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricProducer;

final class MeasuringConsumerManager<P extends MessageMetricProducer<P>> extends ConsumerManager<P> implements
MessageMetricProducer<P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasuringConsumerManager.class);

    private final BidiMap<String, DefaultMessageMetric<? extends Number, P>> metrics = new DualHashBidiMap<String, DefaultMessageMetric<? extends Number, P>>();

    public MeasuringConsumerManager(final P producer) {
        super(producer);
    }

    @Override
    public int countMetrics() {
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

    @Override
    public boolean isMeasuring(final MessageMetric<? extends Number, P> metric) {
        return this.metrics.containsValue(metric);
    }

    @Override
    public boolean isMeasuring(final String id) {
        return this.metrics.containsKey(id);
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
        MeasuringConsumerManager.LOGGER.info("Starting measuring {} in {}.", id, this.getProducer());
        final DefaultMessageMetric<T, P> metric = new DefaultMessageMetric<T, P>(this.getProducer(), measure);
        this.metrics.put(id, metric);
        this.registerConsumer(metric);
        return metric;
    }

    @Override
    public synchronized boolean stop() {
        if (!super.stop()) {
            return false;
        }
        MeasuringConsumerManager.LOGGER.info("Stopping metrics consumer manager for {}.", this.getProducer());
        for (final String metricId : new HashSet<String>(this.metrics.keySet())) {
            this.stopMeasuring(metricId);
        }
        MeasuringConsumerManager.LOGGER.info("Stopped metrics consumer manager for {}.", this.getProducer());
        return true;
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
        MeasuringConsumerManager.LOGGER.info("Stopped measuring {} in {}.", id, this.getProducer());
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MeasuringConsumerManager [");
        if (this.getProducer() != null) {
            builder.append("getProducer()=").append(this.getProducer()).append(", ");
        }
        builder.append("isStopped()=").append(this.isStopped()).append("]");
        return builder.toString();
    }

}
