package com.github.triceo.splitlog;

import java.util.SortedSet;
import java.util.TreeSet;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.SimpleMessageCondition;

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
        String result = LogWriter.write(follower, message1);
        Assertions.assertThat(result).isEqualTo(message1);
        /*
         * when this tag is being created, the first message is already
         * instantiated. therefore, it will come after this message.
         */
        follower.tag(tag1);
        result = LogWriter.write(follower, message2);
        Assertions.assertThat(result).isEqualTo(message2);
        // ditto
        follower.tag(tag2);
        result = LogWriter.write(follower, message3);
        Assertions.assertThat(result).isEqualTo(message3);
        // make sure all messages are present with the default condition
        SortedSet<Message> messages = new TreeSet<>(follower.getMessages());
        DefaultFollowerBaseTest.assertProperOrder(messages, tag0, message1, tag1, message2, tag2);
        // make sure just the tags are present witn nothing-accepting condition
        messages = new TreeSet<>(follower.getMessages(new NothingAcceptingMessageCondition()));
        DefaultFollowerBaseTest.assertProperOrder(messages, tag0, tag1, tag2);
    }

}
