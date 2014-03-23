package com.github.triceo.splitlog.ordering;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.triceo.splitlog.Follower;
import com.github.triceo.splitlog.LogWatch;
import com.github.triceo.splitlog.LogWatchBuilder;
import com.github.triceo.splitlog.LogWriter;
import com.github.triceo.splitlog.Message;
import com.github.triceo.splitlog.splitters.JBossServerLogTailSplitter;

public class MessageOrderingTest {

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
    private final LogWriter writer = new LogWriter(this.target);
    private LogWatch watch;

    @Before
    public void buildWatch() {
        this.watch = LogWatchBuilder.forFile(this.target).buildWith(new JBossServerLogTailSplitter());
    }

    @After
    public void terminateWatch() {
        this.watch.terminate();
        this.watch = null;
    }

    @Test
    public void testOriginalOrdering() {
        final Follower f = this.watch.follow();
        // will make sure all messages from the existing log file are ACCEPTED
        this.writer.write("test", f);
        // messages will be ordered exactly as they came in
        final List<Message> messages = new LinkedList<Message>(f.getMessages());
        Assert.assertEquals(3, messages.size()); // 3 ACCEPTED, 1 INCOMING
        /*
         * message IDs are increased not by 1, but by 2. first for INCOMING,
         * then for ACCEPTED
         */
        Assert.assertEquals(1, messages.get(0).getUniqueId());
        Assert.assertEquals(3, messages.get(1).getUniqueId());
        Assert.assertEquals(5, messages.get(2).getUniqueId());
        this.watch.unfollow(f);
    }

    @Test
    public void testTimeBasedOrdering() {
        final Follower f = this.watch.follow();
        // will make sure all messages from the existing log file are ACCEPTED
        this.writer.write("test", f);
        // messages will be ordered by their timestamp
        final List<Message> messages = new LinkedList<Message>(
                f.getMessages(TimestampOrderingMessageComparator.INSTANCE));
        Assert.assertEquals(3, messages.size()); // 3 ACCEPTED, 1 INCOMING
        Assert.assertEquals(10, messages.get(0).getUniqueId());
        Assert.assertEquals(14, messages.get(1).getUniqueId());
        Assert.assertEquals(12, messages.get(2).getUniqueId());
        this.watch.unfollow(f);
    }
}
