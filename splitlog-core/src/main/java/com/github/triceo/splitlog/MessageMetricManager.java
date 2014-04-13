package com.github.triceo.splitlog;

import java.util.HashSet;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricProducer;
import com.github.triceo.splitlog.api.MessageSource;

public class MessageMetricManager implements MessageMetricProducer {

    private final BidiMap<String, DefaultMessageMetric<? extends Number>> metrics = new DualHashBidiMap<String, DefaultMessageMetric<? extends Number>>();

    @Override
    public synchronized MessageMetric<? extends Number> getMetric(final String id) {
        return this.metrics.get(id);
    }

    @Override
    public synchronized String getMetricId(final MessageMetric<? extends Number> measure) {
        return this.metrics.getKey(measure);
    }

    @Override
    public synchronized <T extends Number> MessageMetric<T> measure(final MessageMeasure<T> measure, final String id) {
        if (this.metrics == null) {
            throw new IllegalArgumentException("Measure may not be null.");
        } else if (id == null) {
            throw new IllegalArgumentException("ID may not be null.");
        } else if (this.metrics.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ID:" + id);
        }
        final DefaultMessageMetric<T> metric = new DefaultMessageMetric<T>(measure);
        this.metrics.put(id, metric);
        return metric;
    }

    synchronized void notifyOfMessage(final Message msg, final MessageDeliveryStatus status, final MessageSource source) {
        for (DefaultMessageMetric<? extends Number> metric : this.metrics.values()) {
            metric.notifyOfMessage(msg, status, source);
        }
    }

    /**
     * Will immediately terminate every measurement that hasn't yet been terminated.
     */
    public synchronized void terminateMeasuring() {
        for (String metricId: new HashSet<String>(this.metrics.keySet())) {
            this.terminateMeasuring(metricId);
        }
    }

    @Override
    public synchronized boolean terminateMeasuring(final MessageMeasure<? extends Number> measure) {
        final String removed = this.metrics.removeValue(measure);
        return (removed != null);
    }
    
    @Override
    public synchronized boolean terminateMeasuring(final String id) {
        final MessageMetric<? extends Number> removed = this.metrics.remove(id);
        return (removed != null);
    }

}
