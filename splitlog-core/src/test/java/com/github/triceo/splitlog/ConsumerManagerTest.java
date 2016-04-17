package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;

public class ConsumerManagerTest extends DefaultFollowerBaseTest {

    private static final String ID = "ID";
    private static final MessageMeasure<Integer, LogWatch> MEASURE = new MessageMeasure<Integer, LogWatch>() {

        @Override
        public Integer initialValue() {
            return 1;
        }

        @Override
        public Integer update(final MessageMetric<Integer, LogWatch> metric, final Message evaluate,
            final MessageDeliveryStatus status, final LogWatch source) {
            final Integer value = metric.getValue();
            return value + 2;
        }

    };

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateId() {
        final ConsumerManager<LogWatch> manager = new ConsumerManager<>(this.getLogWatch());
        manager.startMeasuring(ConsumerManagerTest.MEASURE, ConsumerManagerTest.ID);
        manager.startMeasuring(ConsumerManagerTest.MEASURE, ConsumerManagerTest.ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullId() {
        new ConsumerManager<>(this.getLogWatch()).startMeasuring(ConsumerManagerTest.MEASURE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMeasure() {
        new ConsumerManager<>(this.getLogWatch()).startMeasuring(null, ConsumerManagerTest.ID);
    }

    @Test
    public void testProperRetrieval() {
        final ConsumerManager<LogWatch> manager = new ConsumerManager<>(this.getLogWatch());
        Assertions.assertThat(manager.getMetric(ConsumerManagerTest.ID)).isNull();
        final MessageMetric<Integer, LogWatch> metric = manager.startMeasuring(ConsumerManagerTest.MEASURE,
                ConsumerManagerTest.ID);
        Assertions.assertThat(manager.getMetric(ConsumerManagerTest.ID)).isSameAs(metric);
        Assertions.assertThat(manager.getMetricId(metric)).isSameAs(ConsumerManagerTest.ID);
        Assertions.assertThat(manager.stopMeasuring(ConsumerManagerTest.ID)).isTrue();
        Assertions.assertThat(manager.stopMeasuring(metric)).isFalse();
        Assertions.assertThat(manager.getMetric(ConsumerManagerTest.ID)).isNull();
        Assertions.assertThat(manager.getMetricId(metric)).isNull();
    }

    @Test
    public void testTermination() {
        final ConsumerManager<LogWatch> manager = new ConsumerManager<>(this.getLogWatch());
        Assertions.assertThat(manager.getMetric(ConsumerManagerTest.ID)).isNull();
        // terminate by ID
        MessageMetric<Integer, LogWatch> metric = manager.startMeasuring(ConsumerManagerTest.MEASURE,
                ConsumerManagerTest.ID);
        Assertions.assertThat(manager.stopMeasuring(ConsumerManagerTest.ID)).isTrue();
        Assertions.assertThat(manager.stopMeasuring(metric)).isFalse();
        Assertions.assertThat(manager.getMetric(ConsumerManagerTest.ID)).isNull();
        Assertions.assertThat(manager.getMetricId(metric)).isNull();
        // terminate by metric
        metric = manager.startMeasuring(ConsumerManagerTest.MEASURE, ConsumerManagerTest.ID);
        Assertions.assertThat(manager.stopMeasuring(metric)).isTrue();
        Assertions.assertThat(manager.stopMeasuring(ConsumerManagerTest.ID)).isFalse();
        Assertions.assertThat(manager.getMetric(ConsumerManagerTest.ID)).isNull();
        Assertions.assertThat(manager.getMetricId(metric)).isNull();
        // terminate all
        metric = manager.startMeasuring(ConsumerManagerTest.MEASURE, ConsumerManagerTest.ID);
        manager.stop();
        Assertions.assertThat(manager.getMetric(ConsumerManagerTest.ID)).isNull();
        Assertions.assertThat(manager.getMetricId(metric)).isNull();
    }
}
