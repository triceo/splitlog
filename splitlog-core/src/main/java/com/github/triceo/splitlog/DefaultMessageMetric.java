package com.github.triceo.splitlog;

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageAction;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.expectations.MetricExpectationManager;

final class DefaultMessageMetric<T extends Number, S extends MessageProducer<S>> implements MessageMetric<T, S> {

    private final MetricExpectationManager<T, S> expectations = new MetricExpectationManager<>(this);
    private final MessageMeasure<T, S> measure;
    private final S source;

    private final Long2ObjectSortedMap<Pair<Long, T>> stats = new Long2ObjectRBTreeMap<>();

    public DefaultMessageMetric(final S source, final MessageMeasure<T, S> measure) {
        if (measure == null) {
            throw new IllegalArgumentException("Measure must not be null.");
        } else if (source == null) {
            throw new IllegalArgumentException("Source must not be null.");
        }
        this.source = source;
        this.measure = measure;
    }

    @Override
    public Future<Message> expect(final MessageMetricCondition<T, S> condition) {
        return this.expectations.setExpectation(condition);
    }

    @Override
    public Future<Message> expect(final MessageMetricCondition<T, S> condition, final MessageAction<S> action) {
        return this.expectations.setExpectation(condition);
    }

    @Override
    public MessageMeasure<T, S> getMeasure() {
        return this.measure;
    }

    @Override
    public synchronized long getMessageCount() {
        if (this.stats.isEmpty()) {
            return 0;
        }
        return this.stats.get(this.stats.lastLongKey()).getLeft();
    }

    @Override
    public synchronized long getMessageCount(final Message timestamp) {
        if (timestamp == null) {
            return 0;
        } else if (!this.stats.containsKey(timestamp.getUniqueId())) {
            return -1;
        } else {
            return this.stats.get(timestamp.getUniqueId()).getLeft();
        }
    }

    @Override
    public S getSource() {
        return this.source;
    }

    @Override
    public synchronized T getValue() {
        if (this.stats.isEmpty()) {
            return this.measure.initialValue();
        }
        return this.stats.get(this.stats.lastKey()).getRight();
    }

    @Override
    public synchronized T getValue(final Message timestamp) {
        if (timestamp == null) {
            return this.measure.initialValue();
        } else if (!this.stats.containsKey(timestamp.getUniqueId())) {
            return null;
        } else {
            return this.stats.get(timestamp.getUniqueId()).getRight();
        }
    }

    @Override
    public boolean isStopped() {
        return !this.getSource().isMeasuring(this);
    }

    @Override
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status, final S source) {
        if (this.isStopped()) {
            throw new IllegalStateException("Consumer manager already stopped.");
        } else if ((msg == null) || (status == null) || (source == null)) {
            throw new IllegalArgumentException("Neither message properties may be null.");
        }
        final T newValue = this.measure.update(this, msg, status, source);
        this.stats.put(msg.getUniqueId(), ImmutablePair.of(this.getMessageCount() + 1, newValue));
        if (this.expectations != null) {
            this.expectations.messageReceived(msg, status, source);
        }
    }

    @Override
    public boolean stop() {
        if (this.isStopped()) {
            return false;
        }
        this.expectations.stop();
        return this.getSource().stopMeasuring(this);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("DefaultMessageMetric [");
        if (this.source != null) {
            builder.append("source=").append(this.source).append(", ");
        }
        builder.append("getMessageCount()=").append(this.getMessageCount()).append(", ");
        if (this.getValue() != null) {
            builder.append("getValue()=").append(this.getValue()).append(", ");
        }
        builder.append("isStopped()=").append(this.isStopped()).append("]");
        return builder.toString();
    }

}
