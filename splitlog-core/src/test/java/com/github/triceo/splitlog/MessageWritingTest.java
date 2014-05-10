package com.github.triceo.splitlog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;

/**
 * This test has a weird name, due to the Abstract thing. But this is so that
 * it's always run first in the alphabetical order, and therefore (when parallel
 * surefire is enabled and more than 1 core is available) the run time of all
 * tests will be minimized.
 *
 */
@RunWith(Parameterized.class)
public class MessageWritingTest extends DefaultFollowerBaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageWritingTest.class);
    private static final int MESSAGES_TO_WRITE = 10;

    public MessageWritingTest(final LogWatchBuilder builder) {
        super(builder);
    }

    @Test
    public void testWriteMessages() {
        this.writeAndTest(false);
    }

    @Test
    public void testWriteMessagesAfterTerminated() {
        this.writeAndTest(true);
    }

    private void writeAndTest(final boolean closeBeforeWriting) {
        final Follower follower = this.getLogWatch().startFollowing();
        final List<String> messages = new LinkedList<String>();
        for (int i = 0; i < MessageWritingTest.MESSAGES_TO_WRITE; i++) {
            messages.add(this.getWriter().write(UUID.randomUUID().toString(), follower));
        }
        messages.remove(messages.size() - 1); // last message will not be
        // written
        if (closeBeforeWriting) {
            this.getLogWatch().stopFollowing(follower);
        }
        try {
            final File f = File.createTempFile("splitlog-", ".log");
            MessageWritingTest.LOGGER.info("Will write into '{}'.", f);
            follower.write(new FileOutputStream(f));
            Assertions.assertThat(f).exists();
            final List<String> lines = FileUtils.readLines(f, "UTF-8");
            Assertions.assertThat(lines).isEqualTo(messages);
        } catch (final Exception e) {
            Assertions.fail("Couldn't write to file.");
        } finally {
            if (this.getLogWatch().isFollowedBy(follower)) {
                this.getLogWatch().stopFollowing(follower);
            }
        }
    }

}
