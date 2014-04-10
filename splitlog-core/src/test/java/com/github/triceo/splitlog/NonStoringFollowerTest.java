package com.github.triceo.splitlog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageCondition;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.conditions.AllMessagesAcceptingCondition;

@RunWith(Parameterized.class)
public class NonStoringFollowerTest extends DefaultFollowerBaseTest {

    private static final class NothingAcceptingMessageCondition implements MessageCondition {

        @Override
        public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final Follower source) {
            return false;
        }

        @Override
        public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
            return false;
        }

    }

    private static final class TestStartingMessageCondition implements MessageCondition {

        private boolean accept(final Message evaluate) {
            return (evaluate.getLines().get(0).startsWith("test"));
        }

        @Override
        public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final Follower source) {
            return this.accept(evaluate);
        }

        @Override
        public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
            return this.accept(evaluate);
        }

    }

    private static final class NumberEndingMessageCondition implements MessageCondition {

        private boolean accept(final Message evaluate) {
            final String line = evaluate.getLines().get(0);
            final char endingCharacter = line.charAt(line.length() - 1);
            return Character.isDigit(endingCharacter);
        }

        @Override
        public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final Follower source) {
            return this.accept(evaluate);
        }

        @Override
        public boolean accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
            return this.accept(evaluate);
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStoringFollowerTest.class);
    private static final int MESSAGES_TO_WRITE = 10;

    public NonStoringFollowerTest(final LogWatchBuilder builder) {
        super(builder);
    }

    private void writeAndTest(final boolean closeBeforeWriting) {
        final Follower follower = this.getLogWatch().follow();
        final List<String> messages = new LinkedList<String>();
        for (int i = 0; i < NonStoringFollowerTest.MESSAGES_TO_WRITE; i++) {
            messages.add(this.getWriter().write(UUID.randomUUID().toString(), follower));
        }
        messages.remove(messages.size() - 1); // last message will not be
        // written
        if (closeBeforeWriting) {
            this.getLogWatch().unfollow(follower);
        }
        try {
            final File f = File.createTempFile("splitlog-", ".log");
            NonStoringFollowerTest.LOGGER.info("Will write into '{}'.", f);
            follower.write(new FileOutputStream(f));
            Assertions.assertThat(f).exists();
            final List<String> lines = FileUtils.readLines(f, "UTF-8");
            Assertions.assertThat(lines).isEqualTo(messages);
        } catch (final Exception e) {
            Assertions.fail("Couldn't write to file.");
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
        Assertions.assertThat(result).isEqualTo(message1);
        result = this.getWriter().write(message1, follower);
        Assertions.assertThat(result).isEqualTo(message1);
        result = this.getWriter().write(message2part1 + "\n" + message2part2, follower);
        Assertions.assertThat(result).isEqualTo(message2part2);
        result = this.getWriter().write(message3part1 + "\r\n" + message3part2, follower);
        Assertions.assertThat(result).isEqualTo(message3part2);
        // now validate the results with the default condition
        List<Message> messages = new LinkedList<Message>(follower.getMessages());
        Assertions.assertThat(messages.size()).isEqualTo(5);
        Assertions.assertThat(messages.get(1).getLines().get(0)).isEqualTo(message1);
        Assertions.assertThat(messages.get(2).getLines().get(0)).isEqualTo(message2part1);
        Assertions.assertThat(messages.get(3).getLines().get(0)).isEqualTo(message2part2);
        Assertions.assertThat(messages.get(4).getLines().get(0)).isEqualTo(message3part1);
        // now validate a condition that will accept all messages
        messages = new LinkedList<Message>(follower.getMessages(new TestStartingMessageCondition()));
        Assertions.assertThat(messages.size()).isEqualTo(5);
        Assertions.assertThat(messages.get(1).getLines().get(0)).isEqualTo(message1);
        Assertions.assertThat(messages.get(2).getLines().get(0)).isEqualTo(message2part1);
        Assertions.assertThat(messages.get(3).getLines().get(0)).isEqualTo(message2part2);
        Assertions.assertThat(messages.get(4).getLines().get(0)).isEqualTo(message3part1);
        // now validate a condition that will only accept messages ending with a
        // numeric digit
        messages = new LinkedList<Message>(follower.getMessages(new NumberEndingMessageCondition()));
        Assertions.assertThat(messages.size()).isEqualTo(3);
        Assertions.assertThat(messages.get(0).getLines().get(0)).isEqualTo(message2part1);
        Assertions.assertThat(messages.get(1).getLines().get(0)).isEqualTo(message2part2);
        Assertions.assertThat(messages.get(2).getLines().get(0)).isEqualTo(message3part1);
        // final part of the message, message3part2, will remain unflushed
        this.getLogWatch().unfollow(follower);
        Assertions.assertThat(follower.isFollowing()).isFalse();
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
        Assertions.assertThat(result).isEqualTo(message1);
        /*
         * when this tag is being created, the first message is already
         * instantiated. therefore, it will come after this message.
         */
        follower.tag(tag1);
        result = this.getWriter().write(message2, follower);
        Assertions.assertThat(result).isEqualTo(message2);
        // ditto
        follower.tag(tag2);
        result = this.getWriter().write(message3, follower);
        Assertions.assertThat(result).isEqualTo(message3);
        // make sure all messages are present with the default condition
        List<Message> messages = new LinkedList<Message>(follower.getMessages());
        Assertions.assertThat(messages.size()).isEqualTo(5);
        Assertions.assertThat(messages.get(0).getLines().get(0)).isEqualTo(tag0);
        Assertions.assertThat(messages.get(1).getLines().get(0)).isEqualTo(message1);
        Assertions.assertThat(messages.get(2).getLines().get(0)).isEqualTo(tag1);
        Assertions.assertThat(messages.get(3).getLines().get(0)).isEqualTo(message2);
        Assertions.assertThat(messages.get(4).getLines().get(0)).isEqualTo(tag2);
        // make sure just the tags are present witn nothing-accepting condition
        messages = new LinkedList<Message>(follower.getMessages(new NothingAcceptingMessageCondition()));
        Assertions.assertThat(messages.size()).isEqualTo(3);
        Assertions.assertThat(messages.get(0).getLines().get(0)).isEqualTo(tag0);
        Assertions.assertThat(messages.get(1).getLines().get(0)).isEqualTo(tag1);
        Assertions.assertThat(messages.get(2).getLines().get(0)).isEqualTo(tag2);
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
        Assertions.assertThat(result).isEqualTo(message2);
        Assertions.assertThat(follower.getMessages().size()).isEqualTo(1);
        // start a second follower, send some messages
        final Follower nestedFollower = this.getLogWatch().follow();
        result = this.getWriter().write(message3, follower);
        result = this.getWriter().write(message4, follower);
        Assertions.assertThat(result).isEqualTo(message4);
        this.getLogWatch().unfollow(nestedFollower);
        // send another message, so the original follower has something extra
        Assertions.assertThat(nestedFollower.isFollowing()).isFalse();
        result = this.getWriter().write(message5, follower);
        // and make sure that the original follower has all messages
        final List<Message> messages = new LinkedList<Message>(follower.getMessages());
        Assertions.assertThat(messages.size()).isEqualTo(4);
        Assertions.assertThat(messages.get(0).getLines().get(0)).isEqualTo(message1);
        Assertions.assertThat(messages.get(1).getLines().get(0)).isEqualTo(message2);
        Assertions.assertThat(messages.get(2).getLines().get(0)).isEqualTo(message3);
        Assertions.assertThat(messages.get(3).getLines().get(0)).isEqualTo(message4);
        // and the nested follower has only the two while it was running
        final List<Message> nestedMessages = new LinkedList<Message>(nestedFollower.getMessages());
        Assertions.assertThat(nestedFollower.getMessages().size()).isEqualTo(2);
        Assertions.assertThat(nestedMessages.get(0).getLines().get(0)).isEqualTo(message2);
        Assertions.assertThat(nestedMessages.get(1).getLines().get(0)).isEqualTo(message3);
    }

    @Test
    public void testTermination() {
        Assertions.assertThat(this.getLogWatch().isTerminated()).as("Log watch terminated immediately after starting.")
        .isFalse();
        final Follower follower1 = this.getLogWatch().follow();
        Assertions.assertThat(this.getLogWatch().isFollowedBy(follower1))
        .as("Follower terminated immediately after starting.").isTrue();
        final Follower follower2 = this.getLogWatch().follow();
        Assertions.assertThat(this.getLogWatch().unfollow(follower1)).as("Wrong termination result.").isTrue();
        Assertions.assertThat(this.getLogWatch().unfollow(follower1)).as("Wrong termination result.").isFalse();
        Assertions.assertThat(this.getLogWatch().isFollowedBy(follower2))
        .as("Follower terminated without termination.").isTrue();
        Assertions.assertThat(this.getLogWatch().isFollowedBy(follower1))
        .as("Follower not terminated after termination.").isFalse();
        Assertions.assertThat(this.getLogWatch().terminate()).as("Wrong termination result.").isTrue();
        Assertions.assertThat(this.getLogWatch().terminate()).as("Wrong termination result.").isFalse();
        Assertions.assertThat(this.getLogWatch().isFollowedBy(follower2))
        .as("Follower not terminated after termination.").isFalse();
        Assertions.assertThat(this.getLogWatch().isTerminated()).as("Log watch not terminated after termination.")
        .isTrue();
    }

    @Test
    public void testWaitForAfterPreviousFailed() {
        final Follower follower = this.getLogWatch().follow();
        // this call will fail, since we're not writing anything
        final Message noMessage = follower.waitFor(AllMessagesAcceptingCondition.INSTANCE, 1, TimeUnit.SECONDS);
        Assertions.assertThat(noMessage).isNull();
        // these calls should succeed
        final String message = "test";
        String result = this.getWriter().write(message, follower);
        Assertions.assertThat(result).isEqualTo(message);
        result = this.getWriter().write(message, follower);
        Assertions.assertThat(result).isEqualTo(message);
        final List<Message> messages = new LinkedList<Message>(follower.getMessages());
        Assertions.assertThat(messages.size()).isEqualTo(1);
        Assertions.assertThat(messages.get(0).getLines().get(0)).isEqualTo(message);
        this.getLogWatch().unfollow(follower);
    }

}
