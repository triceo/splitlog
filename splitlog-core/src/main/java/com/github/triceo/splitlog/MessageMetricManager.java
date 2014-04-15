package com.github.triceo.splitlog;

import java.util.HashSet;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricProducer;
import com.github.triceo.splitlog.api.MessageProducer;

final class MessageMetricManager<S extends MessageProducer<S>> extends MessageManager<S> implements
MessageMetricProducer<S>, MessageConsumer<S> {

    private final BidiMap<String, DefaultMessageMetric<? extends Number, S>> metrics = new DualHashBidiMap<String, DefaultMessageMetric<? extends Number, S>>();
    private final S source;

    public MessageMetricManager(final S source) {
        this.source = source;
    }

    @Override
    public synchronized MessageMetric<? extends Number, S> getMetric(final String id) {
        return this.metrics.get(id);
    }

    @Override
    public synchronized String getMetricId(final MessageMetric<? extends Number, S> measure) {
        return this.metrics.getKey(measure);
    }

    @Override
    public boolean isMeasuring(final MessageMetric<? extends Number, S> metric) {
        return this.metrics.containsValue(metric);
    }

    @Override
    public boolean isMeasuring(final String id) {
        return this.metrics.containsKey(id);
    }

    @Override
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status, final S source) {
        for (final DefaultMessageMetric<? extends Number, S> metric : this.metrics.values()) {
            metric.messageReceived(msg, status, source);
        }
    }

    @Override
    public synchronized <T extends Number> MessageMetric<T, S> startMeasuring(final MessageMeasure<T, S> measure,
            final String id) {
        if (measure == null) {
            throw new IllegalArgumentException("Measure may not be null.");
        } else if (id == null) {
            throw new IllegalArgumentException("ID may not be null.");
        } else if (this.metrics.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ID:" + id);
        }
        final DefaultMessageMetric<T, S> metric = new DefaultMessageMetric<T, S>(this.source, measure);
        this.metrics.put(id, metric);
        return metric;
    }

    @Override
    public synchronized boolean stopMeasuring(final MessageMetric<? extends Number, S> measure) {
        final String removed = this.metrics.removeValue(measure);
        return (removed != null);
    }

    @Override
    public synchronized boolean stopMeasuring(final String id) {
        final MessageMetric<? extends Number, S> removed = this.metrics.remove(id);
        return (removed != null);
    }

    /**
     * Will immediately terminate every measurement that hasn't yet been
     * terminated.
     */
    public synchronized void terminateMeasuring() {
        for (final String metricId : new HashSet<String>(this.metrics.keySet())) {
            this.stopMeasuring(metricId);
        }
    }

}
