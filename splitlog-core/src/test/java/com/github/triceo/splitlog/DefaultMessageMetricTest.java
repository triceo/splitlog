package com.github.triceo.splitlog;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;

public class DefaultMessageMetricTest extends DefaultFollowerBaseTest {

    private static final MessageMeasure<Integer, LogWatch> DEFAULT_MEASURE = new MessageMeasure<Integer, LogWatch>() {

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

    private static final MessageBuilder MESSAGE = new MessageBuilder("test");

    @Test
    public void testGetMeasure() {
        final MessageMetric<Integer, LogWatch> metric = new DefaultMessageMetric<>(this.getLogWatch(),
                DefaultMessageMetricTest.DEFAULT_MEASURE);
        Assertions.assertThat(metric.getMeasure()).isSameAs(DefaultMessageMetricTest.DEFAULT_MEASURE);
    }

    @Test
    public void testIncreases() {
        final LogWatch watch = this.getLogWatch();
        final DefaultMessageMetric<Integer, LogWatch> metric = (DefaultMessageMetric<Integer, LogWatch>) watch.startMeasuring(DefaultMessageMetricTest.DEFAULT_MEASURE, "test");
        Assertions.assertThat(metric.getMessageCount(null)).isEqualTo(0);
        Assertions.assertThat(metric.getValue(null)).isEqualTo(1);
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(0);
        Assertions.assertThat(metric.getValue()).isEqualTo(1);
        final Message one = DefaultMessageMetricTest.MESSAGE.buildFinal();
        metric.messageReceived(one, MessageDeliveryStatus.ACCEPTED, watch);
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(1);
        Assertions.assertThat(metric.getValue()).isEqualTo(3);
        final Message two = DefaultMessageMetricTest.MESSAGE.buildFinal();
        metric.messageReceived(two, MessageDeliveryStatus.ACCEPTED, watch);
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(2);
        Assertions.assertThat(metric.getValue()).isEqualTo(5);
        // test history
        Assertions.assertThat(metric.getMessageCount(one)).isEqualTo(1);
        Assertions.assertThat(metric.getValue(one)).isEqualTo(3);
        Assertions.assertThat(metric.getMessageCount(two)).isEqualTo(2);
        Assertions.assertThat(metric.getValue(two)).isEqualTo(5);
        final Message none = DefaultMessageMetricTest.MESSAGE.buildFinal();
        Assertions.assertThat(metric.getMessageCount(none)).isEqualTo(-1);
        Assertions.assertThat(metric.getValue(none)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullConstructor() {
        new DefaultMessageMetric<Integer, LogWatch>(this.getLogWatch(), null);
    }

    @Test
    public void testWaiting() {
        final LogWatch watch = this.getLogWatch(); // placeholder
        final DefaultMessageMetric<Integer, LogWatch> metric = (DefaultMessageMetric<Integer, LogWatch>) watch.startMeasuring(DefaultMessageMetricTest.DEFAULT_MEASURE, "test");
        Assertions.assertThat(metric.getValue()).isEqualTo(1);
        metric.messageReceived(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED, watch);
        Assertions.assertThat(metric.getValue()).isEqualTo(3);
        // prepare the waiting
        final Future<Message> result = metric.expect(evaluate -> evaluate.getValue() == 7);
        // this message will only increase the metric
        metric.messageReceived(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED, watch);
        // this message will put metric over threshold
        final Message expected = DefaultMessageMetricTest.MESSAGE.buildFinal();
        metric.messageReceived(expected, MessageDeliveryStatus.ACCEPTED, watch);
        // and this message will, once again, do nothing; no one is waiting
        metric.messageReceived(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED, watch);
        try {
            Assertions.assertThat(result.get(10, TimeUnit.SECONDS)).isEqualTo(expected);
        } catch (final Exception e1) {
            Assertions.fail("Metric condition not triggered.", e1);
        }
        Assertions.assertThat(metric.getValue()).isEqualTo(9);
    }
}
