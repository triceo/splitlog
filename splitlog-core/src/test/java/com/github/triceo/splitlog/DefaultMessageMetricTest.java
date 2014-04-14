package com.github.triceo.splitlog;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageSource;

public class DefaultMessageMetricTest extends DefaultFollowerBaseTest {

    private static final MessageMeasure<Integer> DEFAULT_MEASURE = new MessageMeasure<Integer>() {

        @Override
        public Integer update(final MessageMetric<Integer> metric, final Message evaluate,
            final MessageDeliveryStatus status, final MessageSource source) {
            final Integer value = metric.getValue();
            return (value == null) ? 1 : value + 2;
        }

    };

    private static final MessageBuilder MESSAGE = new MessageBuilder("test");

    private final ScheduledExecutorService e = Executors.newScheduledThreadPool(1);

    @Test
    public void testGetMeasure() {
        final MessageMetric<Integer> metric = new DefaultMessageMetric<Integer>(
                DefaultMessageMetricTest.DEFAULT_MEASURE);
        Assertions.assertThat(metric.getMeasure()).isSameAs(DefaultMessageMetricTest.DEFAULT_MEASURE);
    }

    @Test
    public void testIncreases() {
        final LogWatch watch = this.getLogWatch(); // placeholder
        final DefaultMessageMetric<Integer> metric = new DefaultMessageMetric<Integer>(
                DefaultMessageMetricTest.DEFAULT_MEASURE);
        Assertions.assertThat(metric.getMessageCount(null)).isEqualTo(0);
        Assertions.assertThat(metric.getValue(null)).isNull();
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(0);
        Assertions.assertThat(metric.getValue()).isNull();
        final Message one = DefaultMessageMetricTest.MESSAGE.buildFinal();
        metric.messageReceived(one, MessageDeliveryStatus.ACCEPTED, watch);
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(1);
        Assertions.assertThat(metric.getValue()).isEqualTo(1);
        final Message two = DefaultMessageMetricTest.MESSAGE.buildFinal();
        metric.messageReceived(two, MessageDeliveryStatus.ACCEPTED, watch);
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(2);
        Assertions.assertThat(metric.getValue()).isEqualTo(3);
        // test history
        Assertions.assertThat(metric.getMessageCount(one)).isEqualTo(1);
        Assertions.assertThat(metric.getValue(one)).isEqualTo(1);
        Assertions.assertThat(metric.getMessageCount(two)).isEqualTo(2);
        Assertions.assertThat(metric.getValue(two)).isEqualTo(3);
        final Message none = DefaultMessageMetricTest.MESSAGE.buildFinal();
        Assertions.assertThat(metric.getMessageCount(none)).isEqualTo(-1);
        Assertions.assertThat(metric.getValue(none)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullConstructor() {
        new DefaultMessageMetric<Integer>(null);
    }

    @Test
    public void testWaiting() {
        final LogWatch watch = this.getLogWatch(); // placeholder
        final DefaultMessageMetric<Integer> metric = new DefaultMessageMetric<Integer>(
                DefaultMessageMetricTest.DEFAULT_MEASURE);
        Assertions.assertThat(metric.getValue()).isNull();
        metric.messageReceived(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED, watch);
        Assertions.assertThat(metric.getValue()).isEqualTo(1);
        final Future<Message> expected = this.e.schedule(new Callable<Message>() {

            @Override
            public Message call() throws Exception {
                // this message will only increase the metric
                metric.messageReceived(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED,
                        watch);
                // this message will put metric over threshold
                final Message m = DefaultMessageMetricTest.MESSAGE.buildFinal();
                metric.messageReceived(m, MessageDeliveryStatus.ACCEPTED, watch);
                // and this message will, once again, do nothing; nothing else
                // is waiting
                metric.messageReceived(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED,
                        watch);
                return m;
            }

        }, 1, TimeUnit.SECONDS);
        final Message result = metric.waitFor(new MessageMetricCondition<Integer>() {

            @Override
            public boolean accept(final MessageMetric<Integer> evaluate) {
                return evaluate.getValue() == 5;
            }

        }, 10, TimeUnit.SECONDS);
        try {
            Assertions.assertThat(result).isEqualTo(expected.get());
        } catch (final Exception e1) {
            Assertions.fail("Metric condition not triggered.", e1);
        }
        Assertions.assertThat(metric.getValue()).isEqualTo(7);
    }
}
