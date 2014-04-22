package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageProducer;

public class ConsumingTest extends DefaultFollowerBaseTest {

    private static final class FailingMessageListener<T extends MessageProducer<T>> implements MessageListener<T> {

        @Override
        public void messageReceived(final Message message, final MessageDeliveryStatus status, final T producer) {
            Assertions.fail("This shouldn't have been executed.");
        }

    }

    @Test
    public void testStopPropagation1() {
        final LogWatch w = this.getLogWatch();
        final MessageConsumer<LogWatch> c = w.startConsuming(new FailingMessageListener<LogWatch>());
        Assertions.assertThat(w.stopConsuming(c)).isTrue();
        Assertions.assertThat(c.isStopped()).isTrue();
    }

    @Test(expected = IllegalStateException.class)
    public void testStopPropagation2() {
        final LogWatch w = this.getLogWatch();
        final Follower f = w.startFollowing();
        final MessageConsumer<Follower> c = f.startConsuming(new FailingMessageListener<Follower>());
        Assertions.assertThat(f.stop()).isTrue();
        Assertions.assertThat(c.isStopped()).isTrue();
        // should throw the exception
        f.messageReceived(new MessageBuilder("test").buildFinal(), MessageDeliveryStatus.ACCEPTED, w);
    }

    @Test(expected = IllegalStateException.class)
    public void testStopPropagation3() {
        final MergingFollower m = this.testStopPropagationWithMerging();
        // should throw the exception
        final Follower f = m.getMerged().iterator().next();
        m.messageReceived(new MessageBuilder("test").buildFinal(), MessageDeliveryStatus.ACCEPTED, f);
    }

    @Test(expected = IllegalStateException.class)
    public void testStopPropagation4() {
        final MergingFollower m = this.testStopPropagationWithMerging();
        // should throw the exception
        final Follower f = m.getMerged().iterator().next();
        f.messageReceived(new MessageBuilder("test").buildFinal(), MessageDeliveryStatus.ACCEPTED, f.getFollowed());
    }

    private MergingFollower testStopPropagationWithMerging() {
        final LogWatch w = this.getLogWatch();
        final Follower f = w.startFollowing();
        final Follower f2 = w.startFollowing();
        final MergingFollower m = f.mergeWith(f2);
        final MessageConsumer<MergingFollower> c = m.startConsuming(new FailingMessageListener<MergingFollower>());
        // main propagation
        Assertions.assertThat(m.stop()).isTrue();
        Assertions.assertThat(c.isStopped()).isTrue();
        // side effects
        Assertions.assertThat(f.isStopped()).isTrue();
        Assertions.assertThat(f.stop()).isFalse();
        Assertions.assertThat(f2.isStopped()).isTrue();
        Assertions.assertThat(f2.stop()).isFalse();
        return m;
    }
}
