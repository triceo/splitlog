package com.github.triceo.splitlog;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricCondition;
import com.github.triceo.splitlog.api.MessageSource;

public class DefaultMessageMetricTest {

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
        final DefaultMessageMetric<Integer> metric = new DefaultMessageMetric<Integer>(
                DefaultMessageMetricTest.DEFAULT_MEASURE);
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(0);
        Assertions.assertThat(metric.getValue()).isNull();
        metric.notifyOfMessage(null, null, null); // shouldn't use nulls, but in
        // this test, it doesn't
        // matter
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(1);
        Assertions.assertThat(metric.getValue()).isEqualTo(1);
        metric.notifyOfMessage(null, null, null);
        Assertions.assertThat(metric.getMessageCount()).isEqualTo(2);
        Assertions.assertThat(metric.getValue()).isEqualTo(3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullConstructor() {
        new DefaultMessageMetric<Integer>(null);
    }

    @Test
    public void testWaiting() {
        final DefaultMessageMetric<Integer> metric = new DefaultMessageMetric<Integer>(
                DefaultMessageMetricTest.DEFAULT_MEASURE);
        Assertions.assertThat(metric.getValue()).isNull();
        metric.notifyOfMessage(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED, null);
        Assertions.assertThat(metric.getValue()).isEqualTo(1);
        final Future<Message> expected = this.e.schedule(new Callable<Message>() {

            @Override
            public Message call() throws Exception {
                // this message will only increase the metric
                metric.notifyOfMessage(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED,
                        null);
                // this message will put metric over threshold
                final Message m = DefaultMessageMetricTest.MESSAGE.buildFinal();
                metric.notifyOfMessage(m, MessageDeliveryStatus.ACCEPTED, null);
                // and this message will, once again, do nothing; nothing else
                // is waiting
                metric.notifyOfMessage(DefaultMessageMetricTest.MESSAGE.buildFinal(), MessageDeliveryStatus.ACCEPTED,
                        null);
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
