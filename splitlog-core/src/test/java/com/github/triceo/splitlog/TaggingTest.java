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
import com.github.triceo.splitlog.api.SimpleMessageCondition;

/**
 * This test has a weird name, due to the Abstract thing. But this is so that
 * it's always run first in the alphabetical order, and therefore (when parallel
 * surefire is enabled and more than 1 core is available) the run time of all
 * tests will be minimized.
 *
 */
@RunWith(Parameterized.class)
public class TaggingTest extends DefaultFollowerBaseTest {

    private static final class NothingAcceptingMessageCondition implements SimpleMessageCondition {

        @Override
        public boolean accept(final Message evaluate) {
            return false;
        }

    }

    public TaggingTest(final LogWatchBuilder builder) {
        super(builder);
    }

    @Test
    public void testTag() {
        final String message1 = "test";
        final String message2 = "test7";
        final String message3 = "test11";
        final String tag0 = "tag1";
        final String tag1 = "tag1";
        final String tag2 = "tag2";
        final Follower follower = this.getLogWatch().startFollowing();
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

}
