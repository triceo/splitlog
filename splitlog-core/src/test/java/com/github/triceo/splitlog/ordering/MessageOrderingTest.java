package com.github.triceo.splitlog.ordering;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.triceo.splitlog.AbstractSplitlogTest;
import com.github.triceo.splitlog.LogWriter;
import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.splitters.JBossServerLogTailSplitter;

public class MessageOrderingTest extends AbstractSplitlogTest {

    private static File createTempCopy(final File toCopy) {
        try {
            final File newTempFile = File.createTempFile("splitlog", "-log");
            FileUtils.copyFile(toCopy, newTempFile);
            return newTempFile;
        } catch (final IOException e) {
            return null;
        }
    }

    private final File target = MessageOrderingTest.createTempCopy(new File(
            "src/test/resources/com/github/triceo/splitlog/ordering/", "ordering.log"));
    private LogWatch watch;
    private final LogWriter writer = LogWriter.forFile(this.target);

    @Before
    public void buildWatch() {
        this.watch = LogWatchBuilder.getDefault().watchingFile(this.target).buildWith(new JBossServerLogTailSplitter());
    }

    @After
    public void terminateWatch() {
        this.watch.terminate();
        this.watch = null;
    }

    @Test
    public void testOriginalOrdering() {
        final Follower f = this.watch.startFollowing();
        // will make sure all messages from the existing log file are ACCEPTED
        this.writer.write("test", f);
        // messages will be ordered exactly as they came in
        final List<Message> messages = new LinkedList<Message>(f.getMessages());
        Assertions.assertThat(messages.size()).isEqualTo(3); // 3 ACCEPTED, 1
        // INCOMING
        Assertions.assertThat(messages.get(0).getUniqueId()).isEqualTo(0);
        Assertions.assertThat(messages.get(1).getUniqueId()).isEqualTo(1);
        Assertions.assertThat(messages.get(2).getUniqueId()).isEqualTo(2);
        this.watch.stopFollowing(f);
    }

    @Test
    public void testTimeBasedOrdering() {
        final Follower f = this.watch.startFollowing();
        // will make sure all messages from the existing log file are ACCEPTED
        this.writer.write("test", f);
        // messages will be ordered by their timestamp
        final List<Message> messages = new LinkedList<Message>(
                f.getMessages(TimestampOrderingMessageComparator.INSTANCE));
        Assertions.assertThat(messages.size()).isEqualTo(3); // 3 ACCEPTED, 1
        // INCOMING
        // number 3 was dropped as INCOMING in previous test method
        Assertions.assertThat(messages.get(0).getUniqueId()).isEqualTo(4);
        Assertions.assertThat(messages.get(1).getUniqueId()).isEqualTo(6);
        Assertions.assertThat(messages.get(2).getUniqueId()).isEqualTo(5);
        this.watch.stopFollowing(f);
    }
}
