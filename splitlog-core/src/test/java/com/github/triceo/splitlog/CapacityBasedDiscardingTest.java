package com.github.triceo.splitlog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.Message;

@RunWith(Parameterized.class)
public class CapacityBasedDiscardingTest extends DefaultFollowerBaseTest {

    public CapacityBasedDiscardingTest(final LogWatchBuilder builder) {
        super(builder.limitCapacityTo(1));
    }

    @Test
    public void test() {
        final Follower follower = this.getLogWatch().follow();
        // tag before any messages
        final Message firstTag = follower.tag("test");
        DefaultFollowerBaseTest.assertProperOrder(follower.getMessages(), firstTag);
        final String firstMessage = "check";
        this.getWriter().write(firstMessage, follower);
        // receive first message, check presence of tag
        final String secondMessage = "check2";
        this.getWriter().write(secondMessage, follower);
        final Message secondTag = follower.tag("test2");
        DefaultFollowerBaseTest.assertProperOrder(follower.getMessages(), firstTag, firstMessage, secondTag);
        // receive second message, discarding first message and not the tag
        final String thirdMessage = "check3";
        this.getWriter().write(thirdMessage, follower);
        DefaultFollowerBaseTest.assertProperOrder(follower.getMessages(), firstTag, secondMessage, secondTag);
        // receive third message, discarding second message and not the tag
        final String fourthMessage = "check4";
        this.getWriter().write(fourthMessage, follower);
        DefaultFollowerBaseTest.assertProperOrder(follower.getMessages(), firstTag, secondTag, thirdMessage);
    }

}
