package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
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

    @Test
    public void testGetMeasure() {
        final MessageMetric<Integer> metric = new DefaultMessageMetric<Integer>(
                DefaultMessageMetricTest.DEFAULT_MEASURE);
        Assertions.assertThat(metric.getMeasure()).isEqualTo(DefaultMessageMetricTest.DEFAULT_MEASURE);
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

}
