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

    private static final class CountingMessageListener<T extends MessageProducer<T>> implements MessageListener<T> {

        private int numCalls = 0;

        public int getNumberOfTimesCalled() {
            return this.numCalls;
        }

        @Override
        public void messageReceived(final Message message, final MessageDeliveryStatus status, final T producer) {
            if (status != MessageDeliveryStatus.INCOMING) {
                return;
            }
            this.numCalls++;
        }

    }

    private static final class FailingMessageListener<T extends MessageProducer<T>> implements MessageListener<T> {

        @Override
        public void messageReceived(final Message message, final MessageDeliveryStatus status, final T producer) {
            Assertions.fail("This shouldn't have been executed.");
        }

    }

    @Test
    public void testCallPropagation1() {
        final DefaultLogWatch w = (DefaultLogWatch) this.getLogWatch();
        final CountingMessageListener<LogWatch> l1 = new CountingMessageListener<>();
        // direct consumption
        w.startConsuming(l1);
        w.messageIncoming(new MessageBuilder("test").buildIntermediate());
        Assertions.assertThat(l1.getNumberOfTimesCalled()).isEqualTo(1);
        // indirection of first order
        final CountingMessageListener<Follower> l2 = new CountingMessageListener<>();
        final Follower f = w.startFollowing();
        f.startConsuming(l2);
        w.messageIncoming(new MessageBuilder("test2").buildIntermediate());
        Assertions.assertThat(l2.getNumberOfTimesCalled()).isEqualTo(1);
        // indirection of second order
        final CountingMessageListener<MergingFollower> l3 = new CountingMessageListener<>();
        final Follower f2 = w.startFollowing();
        final MergingFollower mf = f2.mergeWith(f);
        mf.startConsuming(l3);
        w.messageIncoming(new MessageBuilder("test3").buildIntermediate());
        /*
         * will receive the same message from two followers
         */
        Assertions.assertThat(l3.getNumberOfTimesCalled()).isEqualTo(2);
        /*
         * and check the state of all other consumers
         */
        Assertions.assertThat(l2.getNumberOfTimesCalled()).isEqualTo(2);
        Assertions.assertThat(l1.getNumberOfTimesCalled()).isEqualTo(3);
    }

    @Test
    public void testConsumerEquality() {
        final MessageProducer w = (DefaultLogWatch) this.getLogWatch();
        final MessageListener<LogWatch> l1 = new CountingMessageListener<>();
        final MessageConsumer<LogWatch> c1 = w.startConsuming(l1);
        final MessageConsumer<LogWatch> c2 = w.startConsuming(l1);
        Assertions.assertThat(c2).isSameAs(c1);
        final MessageListener<LogWatch> l2 = new CountingMessageListener<>();
        final MessageConsumer<LogWatch> c3 = w.startConsuming(l2);
        final MessageConsumer<LogWatch> c4 = w.startConsuming(l1);
        Assertions.assertThat(c3).isNotSameAs(c1);
        Assertions.assertThat(c4).isSameAs(c1);
    }

    @Test
    public void testStopPropagation1() {
        final LogWatch w = this.getLogWatch();
        final MessageConsumer<LogWatch> c = w.startConsuming(new FailingMessageListener<>());
        Assertions.assertThat(w.stopConsuming(c)).isTrue();
        Assertions.assertThat(c.isStopped()).isTrue();
    }

    @Test(expected = IllegalStateException.class)
    public void testStopPropagation2() {
        final LogWatch w = this.getLogWatch();
        final Follower f = w.startFollowing();
        final MessageConsumer<Follower> c = f.startConsuming(new FailingMessageListener<>());
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
        final MessageConsumer<MergingFollower> c = m.startConsuming(new FailingMessageListener<>());
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
