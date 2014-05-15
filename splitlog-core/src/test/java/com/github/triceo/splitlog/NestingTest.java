package com.github.triceo.splitlog;

import java.util.LinkedList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;

@RunWith(Parameterized.class)
public class NestingTest extends DefaultFollowerBaseTest {

    public NestingTest(final LogWatchBuilder builder) {
        super(builder);
    }

    @Test
    public void testNesting() {
        final String message1 = "test1";
        final String message2 = "test2";
        final String message3 = "test3";
        final String message4 = "test4";
        final String message5 = "test5";
        final Follower follower = this.getLogWatch().startFollowing();
        // make sure the messages are received by the first follower
        this.getWriter().write(message1, follower);
        String result = this.getWriter().write(message2, follower);
        Assertions.assertThat(result).isEqualTo(message2);
        Assertions.assertThat(follower.getMessages().size()).isEqualTo(1);
        // start a second follower, send some messages
        final Follower nestedFollower = this.getLogWatch().startFollowing();
        result = this.getWriter().write(message3, follower);
        result = this.getWriter().write(message4, follower);
        Assertions.assertThat(result).isEqualTo(message4);
        this.getLogWatch().stopFollowing(nestedFollower);
        // send another message, so the original follower has something extra
        Assertions.assertThat(nestedFollower.isStopped()).isTrue();
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

}
