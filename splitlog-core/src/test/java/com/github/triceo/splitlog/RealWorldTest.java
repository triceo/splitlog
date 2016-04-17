package com.github.triceo.splitlog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageSeverity;
import com.github.triceo.splitlog.splitters.JBossServerLogTailSplitter;

public class RealWorldTest extends AbstractSplitlogTest {

    /**
     * Each ACCEPTED non-WARNING message will increase the count by 1
     */
    private static final MessageMeasure<Integer, LogWatch> ACCEPTED_ERROR = new MessageMeasure<Integer, LogWatch>() {

        @Override
        public Integer initialValue() {
            return 0;
        }

        @Override
        public Integer update(final MessageMetric<Integer, LogWatch> metric, final Message evaluate,
            final MessageDeliveryStatus status, final LogWatch source) {
            if (status != MessageDeliveryStatus.ACCEPTED) {
                return metric.getValue();
            } else if (evaluate.getSeverity() != MessageSeverity.ERROR) {
                return metric.getValue();
            } else {
                return metric.getValue() + 1;
            }
        }

    };

    /**
     * Each ACCEPTED non-WARNING message will increase the count by 1
     */
    private static final MessageMeasure<Integer, LogWatch> ACCEPTED_NONWARNING = new MessageMeasure<Integer, LogWatch>() {

        @Override
        public Integer initialValue() {
            return 0;
        }

        @Override
        public Integer update(final MessageMetric<Integer, LogWatch> metric, final Message evaluate,
            final MessageDeliveryStatus status, final LogWatch source) {
            if (status != MessageDeliveryStatus.ACCEPTED) {
                return metric.getValue();
            } else if (evaluate.getSeverity() == MessageSeverity.WARNING) {
                return metric.getValue();
            } else {
                return metric.getValue() + 1;
            }
        }

    };

    private static final int BASE_TIMEOUT_SECONDS = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(RealWorldTest.class);
    private static final String METRIC_ID = "Accepted Non-WARNING";

    private static final String METRIC_ID_2 = "Accepted ERROR";

    @Test
    public void testRegular() {
        // fill file with random garbage
        final File tmp = LogWriter.createTempFile();
        for (int i = 0; i < 1000; i++) {
            try {
                FileUtils.write(tmp, UUID.randomUUID().toString(), "UTF-8", true);
            } catch (final IOException e) {
                // ignore
            }
        }
        // log watch will accept anything not DEBUG
        final LogWatch watch = LogWatchBuilder.getDefault().watchedFile(tmp).ignoringPreexistingContent()
                .withStorageCondition(evaluate -> evaluate.getSeverity() != MessageSeverity.DEBUG).buildWith(new JBossServerLogTailSplitter());
        // metric will only count everything not WARNING
        final MessageMetric<Integer, LogWatch> nonWarning = watch.startMeasuring(RealWorldTest.ACCEPTED_NONWARNING,
                RealWorldTest.METRIC_ID);
        final MessageMetric<Integer, LogWatch> errors = watch.startMeasuring(RealWorldTest.ACCEPTED_ERROR,
                RealWorldTest.METRIC_ID_2);
        final Follower bothBatches = watch.startFollowing();
        final int messagesInFirst = 66;
        final Future<Message> expectation = nonWarning.expect(evaluate -> evaluate.getValue() == (messagesInFirst - 1));
        try {
            FileUtils.copyInputStreamToFile(RealWorldTest.class.getResourceAsStream("realworld.initial"),
                    watch.getWatchedFile());
            RealWorldTest.LOGGER.info("File copied: {}", watch.getWatchedFile());
        } catch (final IOException e) {
            Assertions.fail("Failed copying file.", e);
        }
        // we expect 65 INFO messages...
        final Message lastInfoInFirstBatch = DefaultFollowerBaseTest.wrapWaiting(expectation,
                RealWorldTest.BASE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertThat(lastInfoInFirstBatch).isNotNull();
        // ... no ERRORs
        Assertions.assertThat(errors.getValue()).isEqualTo(0);
        // ... and 1 extra warning
        Assertions.assertThat(bothBatches.getMessages()).hasSize(messagesInFirst);
        /*
         * last INFO is number 67, as even the 2 rejected messages get numbers;
         * therefore 66 + 2 - 1, as we index messages from 0.
         */
        Assertions.assertThat(lastInfoInFirstBatch.getUniqueId()).isEqualTo(messagesInFirst + 1);
        final Follower onlyLastBatch = watch.startFollowing();
        // ##### now insert the second part #####
        try {
            final OutputStream tempFile = new FileOutputStream(watch.getWatchedFile(), true);
            IOUtils.copy(RealWorldTest.class.getResourceAsStream("realworld.append"), tempFile);
        } catch (final IOException ex) {
            Assertions.fail("Couldn't append into the test file.", ex);
        }
        // expect 20 errors...
        final Message lastError = DefaultFollowerBaseTest.wrapWaiting(
                errors.expect(evaluate -> evaluate.getValue() == 20), RealWorldTest.BASE_TIMEOUT_SECONDS * 5, TimeUnit.SECONDS);
        Assertions.assertThat(lastError).isNotNull();
        Assertions.assertThat(lastError.getUniqueId()).isEqualTo(2599);
        // wait for the last message
        final int lastMessageId = 2672;
        final Message lastMessageInSecondBatch = DefaultFollowerBaseTest.wrapWaiting(
                onlyLastBatch.expect((evaluate, status, source) -> ((status == MessageDeliveryStatus.INCOMING) && (evaluate.getUniqueId() == lastMessageId))));
        Assertions.assertThat(lastMessageInSecondBatch).isNotNull();
        Assertions.assertThat(lastMessageInSecondBatch.getUniqueId()).isEqualTo(lastMessageId);
        Assertions.assertThat(bothBatches.getMessages()).hasSize(lastMessageId - 2);
        /*
         * the above difference of 2 are the 2 DEBUG messages not accepted at
         * the beginning
         */
        Assertions.assertThat(onlyLastBatch.getMessages()).hasSize(bothBatches.getMessages().size() - messagesInFirst);
        watch.stop();
    }

}
