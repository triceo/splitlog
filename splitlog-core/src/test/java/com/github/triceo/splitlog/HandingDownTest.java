package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageSource;

public class HandingDownTest extends DefaultFollowerBaseTest {

    private static final String ID = "ID";
    private static final String ID2 = "ID2";
    private static final MessageMeasure<Integer> MEASURE = new MessageMeasure<Integer>() {

        @Override
        public Integer update(final MessageMetric<Integer> metric, final Message evaluate,
            final MessageDeliveryStatus status, final MessageSource source) {
            final Integer value = metric.getValue();
            return (value == null) ? 1 : value + 2;
        }

    };
    private static final MessageMeasure<Integer> MEASURE2 = new MessageMeasure<Integer>() {

        @Override
        public Integer update(final MessageMetric<Integer> metric, final Message evaluate,
            final MessageDeliveryStatus status, final MessageSource source) {
            final Integer value = metric.getValue();
            return (value == null) ? 1 : value + 1;
        }

    };

    @Test
    public void testDuplicates() {
        final LogWatch watch = this.getLogWatch();
        Assertions.assertThat(watch.beHandingDown(HandingDownTest.MEASURE, HandingDownTest.ID)).isTrue();
        Assertions.assertThat(watch.beHandingDown(HandingDownTest.MEASURE, HandingDownTest.ID2)).isFalse();
        Assertions.assertThat(watch.beHandingDown(HandingDownTest.MEASURE2, HandingDownTest.ID)).isFalse();
    }

    @Test
    public void testHandDown() {
        final LogWatch watch = this.getLogWatch();
        final Follower noHandDowns = watch.follow(); // nothing is handed down
        watch.beHandingDown(HandingDownTest.MEASURE, HandingDownTest.ID);
        final Follower handDown = watch.follow(); // this gets handed down one
        Assertions.assertThat(noHandDowns.getMetric(HandingDownTest.ID)).isNull();
        Assertions.assertThat(handDown.getMetric(HandingDownTest.ID)).isNotNull();
        Assertions.assertThat(handDown.getMetric(HandingDownTest.ID).getMeasure()).isEqualTo(HandingDownTest.MEASURE);
        // stop hand-down
        Assertions.assertThat(watch.stopHandingDown(HandingDownTest.ID)).isTrue();
        Assertions.assertThat(watch.stopHandingDown(HandingDownTest.ID)).isFalse();
        Assertions.assertThat(watch.stopHandingDown(HandingDownTest.MEASURE)).isFalse();
        final Follower noHandDowns2 = watch.follow(); // nothing is handed down
        Assertions.assertThat(noHandDowns.getMetric(HandingDownTest.ID)).isNull();
        Assertions.assertThat(handDown.getMetric(HandingDownTest.ID)).isNotNull();
        Assertions.assertThat(handDown.getMetric(HandingDownTest.ID).getMeasure()).isEqualTo(HandingDownTest.MEASURE);
        Assertions.assertThat(noHandDowns2.getMetric(HandingDownTest.ID)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullID() {
        final LogWatch watch = this.getLogWatch();
        watch.beHandingDown(HandingDownTest.MEASURE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMeasure() {
        final LogWatch watch = this.getLogWatch();
        watch.beHandingDown(null, HandingDownTest.ID);
    }

}
