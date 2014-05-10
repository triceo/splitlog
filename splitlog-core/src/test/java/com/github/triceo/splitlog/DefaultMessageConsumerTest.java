package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;

public class DefaultMessageConsumerTest extends DefaultFollowerBaseTest {

    private static final MessageListener<LogWatch> LISTENER = new MessageListener<LogWatch>() {

        @Override
        public void messageReceived(final Message message, final MessageDeliveryStatus status, final LogWatch producer) {
            return; // doesn't need to do anything
        }

    };

    private static final MessageListener<LogWatch> LISTENER2 = new MessageListener<LogWatch>() {

        @Override
        public void messageReceived(final Message message, final MessageDeliveryStatus status, final LogWatch producer) {
            return; // doesn't need to do anything
        }

    };

    @Test
    public void testEquality() {
        final MessageConsumer<LogWatch> mc1 = new DefaultMessageConsumer<LogWatch>(DefaultMessageConsumerTest.LISTENER, this.getLogWatch());
        final MessageConsumer<LogWatch> mc2 = new DefaultMessageConsumer<LogWatch>(DefaultMessageConsumerTest.LISTENER, this.getLogWatch());
        Assertions.assertThat(mc2).isEqualTo(mc1);
        Assertions.assertThat(mc1).isEqualTo(mc2);
        Assertions.assertThat(mc1).isEqualTo(mc1);
        Assertions.assertThat(mc2).isEqualTo(mc2);
    }

    @Test
    public void testInequalityByListener() {
        final MessageConsumer<LogWatch> mc1 = new DefaultMessageConsumer<LogWatch>(DefaultMessageConsumerTest.LISTENER,
                this.getLogWatch());
        final MessageConsumer<LogWatch> mc2 = new DefaultMessageConsumer<LogWatch>(
                DefaultMessageConsumerTest.LISTENER2, this.getLogWatch());
        Assertions.assertThat(mc2).isNotEqualTo(mc1);
        Assertions.assertThat(mc1).isNotEqualTo(mc2);
    }

    @Test
    public void testInequalityByProducer() {
        final MessageConsumer<LogWatch> mc1 = new DefaultMessageConsumer<LogWatch>(DefaultMessageConsumerTest.LISTENER,
                this.getLogWatch());
        final MessageConsumer<LogWatch> mc2 = new DefaultMessageConsumer<LogWatch>(DefaultMessageConsumerTest.LISTENER,
                this.getBuilder().build());
        Assertions.assertThat(mc2).isNotEqualTo(mc1);
        Assertions.assertThat(mc1).isNotEqualTo(mc2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullListener() {
        new DefaultMessageConsumer<LogWatch>(null, this.getLogWatch());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullProducer() {
        new DefaultMessageConsumer<LogWatch>(DefaultMessageConsumerTest.LISTENER, null);
    }

}
