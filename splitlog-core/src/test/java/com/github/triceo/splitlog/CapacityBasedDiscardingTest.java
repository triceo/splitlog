package com.github.triceo.splitlog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;

@RunWith(Parameterized.class)
public class CapacityBasedDiscardingTest extends DefaultFollowerBaseTest {

    public CapacityBasedDiscardingTest(final LogWatchBuilder builder) {
        super(builder.limitCapacityTo(1));
    }

    @Test
    public void test() {
        final Follower follower = this.getLogWatch().startFollowing();
        // tag before any messages
        final Message firstTag = follower.tag("test");
        DefaultFollowerBaseTest.assertProperOrder(follower.getMessages(), firstTag);
        final String firstMessage = "check";
        LogWriter.write(follower, firstMessage);
        // receive first message, check presence of tag
        final String secondMessage = "check2";
        LogWriter.write(follower, secondMessage);
        final Message secondTag = follower.tag("test2");
        DefaultFollowerBaseTest.assertProperOrder(follower.getMessages(), firstTag, firstMessage, secondTag);
        // receive second message, discarding first message and not the tag
        final String thirdMessage = "check3";
        LogWriter.write(follower, thirdMessage);
        DefaultFollowerBaseTest.assertProperOrder(follower.getMessages(), firstTag, secondMessage, secondTag);
        // receive third message, discarding second message and not the tag
        final String fourthMessage = "check4";
        LogWriter.write(follower, fourthMessage);
        DefaultFollowerBaseTest.assertProperOrder(follower.getMessages(), firstTag, secondTag, thirdMessage);
    }

}
