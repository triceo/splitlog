package com.github.triceo.splitlog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;

public abstract class AbstractMessageWritingTest extends DefaultFollowerBaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageWritingTest.class);
    private static final int MESSAGES_TO_WRITE = 10;

    public AbstractMessageWritingTest(final LogWatchBuilder builder) {
        super(builder);
    }

    @Test
    public abstract void testWriteMessages();

    protected void writeAndTest(final boolean closeBeforeWriting) {
        final Follower follower = this.getLogWatch().startFollowing();
        final List<String> messages = new LinkedList<>();
        for (int i = 0; i < AbstractMessageWritingTest.MESSAGES_TO_WRITE; i++) {
            messages.add(LogWriter.write(follower, UUID.randomUUID().toString()));
        }
        messages.remove(messages.size() - 1); // last message will not be
        // written
        if (closeBeforeWriting) {
            this.getLogWatch().stopFollowing(follower);
        }
        try {
            final File f = LogWriter.createTempFile();
            AbstractMessageWritingTest.LOGGER.info("Will write into '{}'.", f);
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
