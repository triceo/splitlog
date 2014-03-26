package com.github.triceo.splitlog;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;

@RunWith(Parameterized.class)
public class NonStoringFollowerTest extends DefaultFollowerBaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringFollowerTest.class);
    private static final File WRITE_MESSAGES_CANONICAL_RESULT = new File(
            "src/test/resources/com/github/triceo/splitlog", "nonstoringfollowertest-testwritemessages.log");

    public NonStoringFollowerTest(final LogWatchBuilder builder) {
        super(builder);
    }

    private void writeAndTest(final boolean closeBeforeWriting) {
        Assert.assertTrue(NonStoringFollowerTest.WRITE_MESSAGES_CANONICAL_RESULT.exists());
        final Follower follower = this.getLogWatch().follow();
        for (final String message : new String[] { "will be written", "will also be written", "will not be written" }) {
            this.getWriter().write(message, follower);
        }
        if (closeBeforeWriting) {
            this.getLogWatch().unfollow(follower);
        }
        try {
            final File f = File.createTempFile("splitlog-", ".log");
            NonStoringFollowerTest.LOGGER.info("Will write into '{}'.", f);
            follower.write(new FileOutputStream(f));
            Assert.assertTrue(f.exists());
            final boolean filesEqual = FileUtils.contentEqualsIgnoreEOL(f,
                    NonStoringFollowerTest.WRITE_MESSAGES_CANONICAL_RESULT, "UTF-8");
            Assert.assertTrue(filesEqual);
        } catch (final Exception e) {
            Assert.fail("Couldn't write to file.");
        } finally {
            if (this.getLogWatch().isFollowedBy(follower)) {
                this.getLogWatch().unfollow(follower);
            }
        }
    }

    @Test
    public void testWriteMessages() {
        this.writeAndTest(false);
    }

    @Test
    public void testWriteMessagesAfterTerminated() {
        this.writeAndTest(true);
    }

    @Test
    public void testGetMessages() {
        final String message1 = "test";
        final String message2part1 = "test1";
        final String message2part2 = "test2";
        final String message3part1 = "test3";
        final String message3part2 = "test4";
        final Follower follower = this.getLogWatch().follow();
        // test simple messages
        String result = this.getWriter().write(message1, follower);
        Assert.assertEquals(message1, result);
        result = this.getWriter().write(message1, follower);
        Assert.assertEquals(message1, result);
        result = this.getWriter().write(message2part1 + "\n" + message2part2, follower);
        Assert.assertEquals(message2part2, result);
        result = this.getWriter().write(message3part1 + "\r\n" + message3part2, follower);
        Assert.assertEquals(message3part2, result);
        // now validate the results
        final List<Message> messages = follower.getMessages();
        Assert.assertEquals(5, messages.size());
        Assert.assertEquals(message1, messages.get(1).getLines().get(0));
        Assert.assertEquals(message2part1, messages.get(2).getLines().get(0));
        Assert.assertEquals(message2part2, messages.get(3).getLines().get(0));
        Assert.assertEquals(message3part1, messages.get(4).getLines().get(0));
        // final part of the message, message3part2, will remain unflushed
        this.getLogWatch().unfollow(follower);
        Assert.assertFalse(follower.isFollowing());
    }

    @Test
    public void testTag() {
        final String message1 = "test";
        final String message2 = "test7";
        final String message3 = "test11";
        final String tag0 = "tag1";
        final String tag1 = "tag1";
        final String tag2 = "tag2";
        final Follower follower = this.getLogWatch().follow();
        follower.tag(tag0);
        String result = this.getWriter().write(message1, follower);
        Assert.assertEquals(message1, result);
        result = this.getWriter().write(message2, follower);
        Assert.assertEquals(message2, result);
        follower.tag(tag1); // message1 will only be written after message2 has
                            // been sent
        result = this.getWriter().write(message3, follower);
        Assert.assertEquals(message3, result);
        follower.tag(tag2); // message2 will only be written after message3 has
                            // been sent
        final List<Message> messages = follower.getMessages();
        Assert.assertEquals(5, messages.size());
        Assert.assertEquals(tag0, messages.get(0).getLines().get(0));
        Assert.assertEquals(message1, messages.get(1).getLines().get(0));
        Assert.assertEquals(tag1, messages.get(2).getLines().get(0));
        Assert.assertEquals(message2, messages.get(3).getLines().get(0));
        Assert.assertEquals(tag2, messages.get(4).getLines().get(0));
    }

    @Test
    public void testNesting() {
        final String message1 = "test1";
        final String message2 = "test2";
        final String message3 = "test3";
        final String message4 = "test4";
        final String message5 = "test5";
        final Follower follower = this.getLogWatch().follow();
        // make sure the messages are received by the first follower
        this.getWriter().write(message1, follower);
        String result = this.getWriter().write(message2, follower);
        Assert.assertEquals(message2, result);
        Assert.assertEquals(1, follower.getMessages().size());
        // start a second follower, send some messages
        final Follower nestedTailer = this.getLogWatch().follow();
        result = this.getWriter().write(message3, follower);
        result = this.getWriter().write(message4, follower);
        Assert.assertEquals(message4, result);
        this.getLogWatch().unfollow(nestedTailer);
        // send another message, so the original follower has something extra
        Assert.assertFalse(nestedTailer.isFollowing());
        result = this.getWriter().write(message5, follower);
        // and make sure that the original follower has all messages
        Assert.assertEquals(4, follower.getMessages().size());
        Assert.assertEquals(message1, follower.getMessages().get(0).getLines().get(0));
        Assert.assertEquals(message2, follower.getMessages().get(1).getLines().get(0));
        Assert.assertEquals(message3, follower.getMessages().get(2).getLines().get(0));
        Assert.assertEquals(message4, follower.getMessages().get(3).getLines().get(0));
        // and the nested follower has only the two while it was running
        Assert.assertEquals(2, nestedTailer.getMessages().size());
        Assert.assertEquals(message2, nestedTailer.getMessages().get(0).getLines().get(0));
        Assert.assertEquals(message3, nestedTailer.getMessages().get(1).getLines().get(0));
    }

    @Test
    public void testTermination() {
        Assert.assertFalse("Log watch terminated immediately after starting.", this.getLogWatch().isTerminated());
        final Follower follower1 = this.getLogWatch().follow();
        Assert.assertTrue("Follower terminated immediately after starting.", this.getLogWatch().isFollowedBy(follower1));
        final Follower follower2 = this.getLogWatch().follow();
        Assert.assertTrue("Wrong termination result.", this.getLogWatch().unfollow(follower1));
        Assert.assertFalse("Wrong termination result.", this.getLogWatch().unfollow(follower1));
        Assert.assertTrue("Follower terminated without termination.", this.getLogWatch().isFollowedBy(follower2));
        Assert.assertFalse("Follower not terminated after termination.", this.getLogWatch().isFollowedBy(follower1));
        Assert.assertTrue("Wrong termination result.", this.getLogWatch().terminate());
        Assert.assertFalse("Wrong termination result.", this.getLogWatch().terminate());
        Assert.assertFalse("Follower not terminated after termination.", this.getLogWatch().isFollowedBy(follower2));
        Assert.assertTrue("Log watch not terminated after termination.", this.getLogWatch().isTerminated());
    }

    @Test
    public void testWaitForAfterPreviousFailed() {
        final Follower follower = this.getLogWatch().follow();
        // this call will fail, since we're not writing anything
        final Message noMessage = follower.waitFor(new MessageDeliveryCondition() {

            @Override
            public boolean accept(final Message evaluate, final MessageDeliveryStatus status) {
                return true;
            }

        }, 1, TimeUnit.SECONDS);
        Assert.assertNull(noMessage);
        // these calls should succeed
        final String message = "test";
        String result = this.getWriter().write(message, follower);
        Assert.assertEquals(message, result);
        result = this.getWriter().write(message, follower);
        Assert.assertEquals(message, result);
        final List<Message> messages = follower.getMessages();
        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(message, messages.get(0).getLines().get(0));
        this.getLogWatch().unfollow(follower);
    }

}
