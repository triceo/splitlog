package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;

public class MessageMetricManagerTest extends DefaultFollowerBaseTest {

    private static final String ID = "ID";
    private static final MessageMeasure<Integer, LogWatch> MEASURE = new MessageMeasure<Integer, LogWatch>() {

        @Override
        public Integer update(final MessageMetric<Integer, LogWatch> metric, final Message evaluate,
            final MessageDeliveryStatus status, final LogWatch source) {
            final Integer value = metric.getValue();
            return (value == null) ? 1 : value + 2;
        }

    };

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateId() {
        final MessageMetricManager<LogWatch> manager = new MessageMetricManager<LogWatch>(this.getLogWatch());
        manager.startMeasuring(MessageMetricManagerTest.MEASURE, MessageMetricManagerTest.ID);
        manager.startMeasuring(MessageMetricManagerTest.MEASURE, MessageMetricManagerTest.ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullId() {
        new MessageMetricManager<LogWatch>(this.getLogWatch()).startMeasuring(MessageMetricManagerTest.MEASURE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMeasure() {
        new MessageMetricManager<LogWatch>(this.getLogWatch()).startMeasuring(null, MessageMetricManagerTest.ID);
    }

    @Test
    public void testProperRetrieval() {
        final MessageMetricManager<LogWatch> manager = new MessageMetricManager<LogWatch>(this.getLogWatch());
        Assertions.assertThat(manager.getMetric(MessageMetricManagerTest.ID)).isNull();
        final MessageMetric<Integer, LogWatch> metric = manager.startMeasuring(MessageMetricManagerTest.MEASURE,
                MessageMetricManagerTest.ID);
        Assertions.assertThat(manager.getMetric(MessageMetricManagerTest.ID)).isSameAs(metric);
        Assertions.assertThat(manager.getMetricId(metric)).isSameAs(MessageMetricManagerTest.ID);
        Assertions.assertThat(manager.stopMeasuring(MessageMetricManagerTest.ID)).isTrue();
        Assertions.assertThat(manager.stopMeasuring(metric)).isFalse();
        Assertions.assertThat(manager.getMetric(MessageMetricManagerTest.ID)).isNull();
        Assertions.assertThat(manager.getMetricId(metric)).isNull();
    }

    @Test
    public void testTermination() {
        final MessageMetricManager<LogWatch> manager = new MessageMetricManager<LogWatch>(this.getLogWatch());
        Assertions.assertThat(manager.getMetric(MessageMetricManagerTest.ID)).isNull();
        // terminate by ID
        MessageMetric<Integer, LogWatch> metric = manager.startMeasuring(MessageMetricManagerTest.MEASURE,
                MessageMetricManagerTest.ID);
        Assertions.assertThat(manager.stopMeasuring(MessageMetricManagerTest.ID)).isTrue();
        Assertions.assertThat(manager.stopMeasuring(metric)).isFalse();
        Assertions.assertThat(manager.getMetric(MessageMetricManagerTest.ID)).isNull();
        Assertions.assertThat(manager.getMetricId(metric)).isNull();
        // terminate by metric
        metric = manager.startMeasuring(MessageMetricManagerTest.MEASURE, MessageMetricManagerTest.ID);
        Assertions.assertThat(manager.stopMeasuring(metric)).isTrue();
        Assertions.assertThat(manager.stopMeasuring(MessageMetricManagerTest.ID)).isFalse();
        Assertions.assertThat(manager.getMetric(MessageMetricManagerTest.ID)).isNull();
        Assertions.assertThat(manager.getMetricId(metric)).isNull();
        // terminate all
        metric = manager.startMeasuring(MessageMetricManagerTest.MEASURE, MessageMetricManagerTest.ID);
        manager.terminateMeasuring();
        Assertions.assertThat(manager.getMetric(MessageMetricManagerTest.ID)).isNull();
        Assertions.assertThat(manager.getMetricId(metric)).isNull();
    }
}
