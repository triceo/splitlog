package com.github.triceo.splitlog;

import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.TailSplitter;
import com.github.triceo.splitlog.conditions.AllFollowerMessagesAcceptingCondition;
import com.github.triceo.splitlog.conditions.SplitlogMessagesRejectingCondition;
import com.github.triceo.splitlog.splitters.JBossServerLogTailSplitter;
import com.github.triceo.splitlog.splitters.SimpleTailSplitter;

@RunWith(Parameterized.class)
public class GatingTest extends DefaultFollowerBaseTest {

    private static final String MEASURE_ID = "test";

    // will verify various configs of log watch
    @Parameters(name = "{index}: {0}, {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile()), false },
                {
                        LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile())
                                .withGateCondition(SplitlogMessagesRejectingCondition.INSTANCE), false },
                {
                        LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile())
                                .withGateCondition(AllFollowerMessagesAcceptingCondition.INSTANCE), true } });
    }

    private final boolean isGatingDisabled;

    public GatingTest(final LogWatchBuilder builder, final boolean gatingDisabled) {
        super(builder);
        this.isGatingDisabled = gatingDisabled;
    }

    private MessageMetric<Integer, LogWatch> fillWithData(final DefaultLogWatch watch, final TailSplitter splitter) {
        final MessageMetric<Integer, LogWatch> metric = watch.startMeasuring(new MessageMeasure<Integer, LogWatch>() {

            @Override
            public Integer initialValue() {
                return 0;
            }

            @Override
            public Integer update(final MessageMetric<Integer, LogWatch> metric, final Message evaluate,
                final MessageDeliveryStatus status, final LogWatch source) {
                if (status != MessageDeliveryStatus.INCOMING) {
                    return metric.getValue() + 1;
                } else {
                    return metric.getValue();
                }
            }

        }, GatingTest.MEASURE_ID);
        watch.messageArrived(this.getMessage(
                "07:30:02,670 INFO  [org.jboss.msc] (main) JBoss MSC version 1.0.4.GA-redhat-1", splitter));
        watch.messageArrived(this.getMessage("07:30:02,670 INFO  [com.github.triceo.splitlog] (main) random", splitter));
        watch.messageArrived(this.getMessage("07:30:02,670 INFO  [com.github.triceo.splitlog.check] (main) random 2",
                splitter));
        watch.messageArrived(this.getMessage(
                "07:30:02,739 DEBUG [org.jboss.as.config] (MSC service thread 1-7) Configured system properties:",
                splitter));
        watch.messageIncoming(this
                .getMessage(
                        "07:30:02,731 INFO  [org.jboss.as] (MSC service thread 1-7) JBAS015899: JBoss BRMS 6.0.1.GA (AS 7.2.1.Final-redhat-10) starting",
                        splitter));
        watch.stop();
        return metric;
    }

    private Message getMessage(final String line, final TailSplitter splitter) {
        return new MessageBuilder(line).buildFinal(splitter);
    }

    private TailSplitter getProperSplitter() {
        return (this.isGatingDisabled) ? new SimpleTailSplitter() : new JBossServerLogTailSplitter();
    }

    @Test
    public void testDefaults() {
        // with simple tail splitter, no gatting can take place
        final DefaultLogWatch watch = (DefaultLogWatch) this.getLogWatch();
        final MessageMetric<Integer, LogWatch> metric = this.fillWithData(watch, new SimpleTailSplitter());
        Assertions.assertThat(metric.getValue()).isEqualTo(4);
    }

    @Test
    public void testGatingCondition() {

        final DefaultLogWatch watch = (DefaultLogWatch) this.getBuilder().buildWith(new JBossServerLogTailSplitter());
        final MessageMetric<Integer, LogWatch> metric = this.fillWithData(watch, this.getProperSplitter());
        if (this.isGatingDisabled) {
            // all messages will be let through
            Assertions.assertThat(metric.getValue()).isEqualTo(4);
        } else {
            // only messages not from splitlog will be let through
            Assertions.assertThat(metric.getValue()).isEqualTo(2);
        }
    }
}
