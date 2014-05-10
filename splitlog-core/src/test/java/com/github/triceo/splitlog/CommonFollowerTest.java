package com.github.triceo.splitlog;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.conditions.AllFollowerMessagesAcceptingCondition;

/**
 * This test has a weird name, due to the Abstract thing. But this is so that
 * it's always run first in the alphabetical order, and therefore (when parallel
 * surefire is enabled and more than 1 core is available) the run time of all
 * tests will be minimized.
 *
 */
@RunWith(Parameterized.class)
public class CommonFollowerTest extends DefaultFollowerBaseTest {

    private static final class NumberEndingMessageCondition implements SimpleMessageCondition {

        @Override
        public boolean accept(final Message evaluate) {
            final String line = evaluate.getLines().get(0);
            final char endingCharacter = line.charAt(line.length() - 1);
            return Character.isDigit(endingCharacter);
        }

    }

    private static final class TestStartingMessageCondition implements SimpleMessageCondition {

        @Override
        public boolean accept(final Message evaluate) {
            return (evaluate.getLines().get(0).startsWith("test"));
        }

    }

    public CommonFollowerTest(final LogWatchBuilder builder) {
        super(builder);
    }

    @Test
    public void testGetMessages() {
        final String message1 = "test";
        final String message2part1 = "test1";
        final String message2part2 = "test2";
        final String message3part1 = "test3";
        final String message3part2 = "test4";
        final Follower follower = this.getLogWatch().startFollowing();
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
        this.getLogWatch().stopFollowing(follower);
        Assertions.assertThat(follower.isStopped()).isTrue();
    }

    @Test
    public void testWaitForAfterPreviousFailed() {
        final Follower follower = this.getLogWatch().startFollowing();
        // this call will fail, since we're not writing anything
        final Message noMessage = DefaultFollowerBaseTest.wrapWaiting(
                follower.expect(AllFollowerMessagesAcceptingCondition.INSTANCE), 1, TimeUnit.SECONDS);
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
        this.getLogWatch().stopFollowing(follower);
    }

}
