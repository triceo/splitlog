package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageListener;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DefaultMessageConsumerTest extends DefaultFollowerBaseTest {

    private static final MessageListener<LogWatch> LISTENER = (message, status, producer) -> {
        // doesn't need to do anything
    };

    private static final MessageListener<LogWatch> LISTENER2 = (message, status, producer) -> {
        // doesn't need to do anything
    };

    @Test
    public void testEquality() {
        final MessageConsumer<LogWatch> mc1 = new DefaultMessageConsumer<>(DefaultMessageConsumerTest.LISTENER, this.getLogWatch());
        final MessageConsumer<LogWatch> mc2 = new DefaultMessageConsumer<>(DefaultMessageConsumerTest.LISTENER, this.getLogWatch());
        Assertions.assertThat(mc2).isEqualTo(mc1);
        Assertions.assertThat(mc1).isEqualTo(mc2);
        Assertions.assertThat(mc1).isEqualTo(mc1);
        Assertions.assertThat(mc2).isEqualTo(mc2);
    }

    @Test
    public void testInequalityByListener() {
        final MessageConsumer<LogWatch> mc1 = new DefaultMessageConsumer<>(DefaultMessageConsumerTest.LISTENER,
                this.getLogWatch());
        final MessageConsumer<LogWatch> mc2 = new DefaultMessageConsumer<>(
                DefaultMessageConsumerTest.LISTENER2, this.getLogWatch());
        Assertions.assertThat(mc2).isNotEqualTo(mc1);
        Assertions.assertThat(mc1).isNotEqualTo(mc2);
    }

    @Test
    public void testInequalityByProducer() {
        final MessageConsumer<LogWatch> mc1 = new DefaultMessageConsumer<>(DefaultMessageConsumerTest.LISTENER,
                this.getLogWatch());
        final MessageConsumer<LogWatch> mc2 = new DefaultMessageConsumer<>(DefaultMessageConsumerTest.LISTENER,
                this.getBuilder().build());
        Assertions.assertThat(mc2).isNotEqualTo(mc1);
        Assertions.assertThat(mc1).isNotEqualTo(mc2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullListener() {
        new DefaultMessageConsumer<>(null, this.getLogWatch());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullProducer() {
        new DefaultMessageConsumer<>(DefaultMessageConsumerTest.LISTENER, null);
    }

}
