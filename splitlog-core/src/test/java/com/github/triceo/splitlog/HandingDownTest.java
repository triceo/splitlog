package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;

public class HandingDownTest extends DefaultFollowerBaseTest {

    private static final String ID = "ID";
    private static final String ID2 = "ID2";
    private static final MessageMeasure<Integer, Follower> MEASURE = new MessageMeasure<Integer, Follower>() {

        @Override
        public Integer initialValue() {
            return 1;
        }

        @Override
        public Integer update(final MessageMetric<Integer, Follower> metric, final Message evaluate,
            final MessageDeliveryStatus status, final Follower source) {
            final Integer value = metric.getValue();
            return (value == null) ? 1 : value + 2;
        }

    };
    private static final MessageMeasure<Integer, Follower> MEASURE2 = new MessageMeasure<Integer, Follower>() {

        @Override
        public Integer initialValue() {
            return 1;
        }

        @Override
        public Integer update(final MessageMetric<Integer, Follower> metric, final Message evaluate,
            final MessageDeliveryStatus status, final Follower source) {
            final Integer value = metric.getValue();
            return (value == null) ? 1 : value + 1;
        }

    };

    @Test
    public void testDuplicates() {
        final LogWatch watch = this.getLogWatch();
        Assertions.assertThat(watch.startHandingDown(HandingDownTest.MEASURE, HandingDownTest.ID)).isTrue();
        Assertions.assertThat(watch.startHandingDown(HandingDownTest.MEASURE, HandingDownTest.ID2)).isFalse();
        Assertions.assertThat(watch.startHandingDown(HandingDownTest.MEASURE2, HandingDownTest.ID)).isFalse();
    }

    @Test
    public void testHandDown() {
        final LogWatch watch = this.getLogWatch();
        final Follower noHandDowns = watch.startFollowing(); // nothing is
        // handed down
        watch.startHandingDown(HandingDownTest.MEASURE, HandingDownTest.ID);
        final Follower handDown = watch.startFollowing(); // this gets handed
        // down one
        final MessageMetric<? extends Number, Follower> handedDown = handDown.getMetric(HandingDownTest.ID);
        Assertions.assertThat(noHandDowns.getMetric(HandingDownTest.ID)).isNull();
        Assertions.assertThat(handedDown).isNotNull();
        Assertions.assertThat(handedDown.getMeasure()).isEqualTo(HandingDownTest.MEASURE);
        Assertions.assertThat(handDown.isMeasuring(HandingDownTest.ID)).isTrue();
        Assertions.assertThat(handDown.isMeasuring(handedDown)).isTrue();
        // stop hand-down
        Assertions.assertThat(watch.stopHandingDown(HandingDownTest.ID)).isTrue();
        Assertions.assertThat(watch.stopHandingDown(HandingDownTest.ID)).isFalse();
        Assertions.assertThat(watch.stopHandingDown(HandingDownTest.MEASURE)).isFalse();
        final Follower noHandDowns2 = watch.startFollowing(); // nothing is
        // handed down
        Assertions.assertThat(noHandDowns.getMetric(HandingDownTest.ID)).isNull();
        Assertions.assertThat(handDown.getMetric(HandingDownTest.ID)).isNotNull();
        Assertions.assertThat(handDown.getMetric(HandingDownTest.ID).getMeasure()).isEqualTo(HandingDownTest.MEASURE);
        Assertions.assertThat(noHandDowns2.getMetric(HandingDownTest.ID)).isNull();
        // and test stopping
        Assertions.assertThat(handDown.isMeasuring(HandingDownTest.ID)).isTrue();
        Assertions.assertThat(handDown.isMeasuring(handedDown)).isTrue();
        final MessageMetric<? extends Number, Follower> handedDown2 = handDown.getMetric(HandingDownTest.ID);
        Assertions.assertThat(handedDown2.stop()).isTrue();
        Assertions.assertThat(handDown.isMeasuring(HandingDownTest.ID)).isFalse();
        Assertions.assertThat(handDown.isMeasuring(handedDown2)).isFalse();
        Assertions.assertThat(handDown.getMetric(HandingDownTest.ID)).isNull();
        Assertions.assertThat(handDown.getMetricId(handedDown2)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullID() {
        final LogWatch watch = this.getLogWatch();
        watch.startHandingDown(HandingDownTest.MEASURE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMeasure() {
        final LogWatch watch = this.getLogWatch();
        watch.startHandingDown(null, HandingDownTest.ID);
    }

}
