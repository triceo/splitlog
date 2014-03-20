package com.github.triceo.splitlog;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ReferenceBasedDiscardingTest extends DefaultFollowerBaseTest {

    public ReferenceBasedDiscardingTest(final LogWatchBuilder builder) {
        super(builder.withDelayBetweenSweeps(1, TimeUnit.SECONDS));
    }

    @Test
    public void test() {
        final DefaultLogWatch w = (DefaultLogWatch) this.getLogWatch();
        Follower follower = w.follow();
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
        // start second follower; this one will only track second+ messages
        Follower follower2 = w.follow();
        // send third message, receive second
        final String thirdMessage = "check3";
        this.getWriter().write(thirdMessage, follower2);
        Assert.assertEquals(2, w.countMessagesInStorage());
        /*
         * remove all references to the first follower; the first message now
         * has no followers available and can be GC'd
         */
        w.unfollow(follower);
        follower = null;
        System.gc();
        final long delay = w.getDelayBetweenSweeps() + 500;
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
            Assert.fail("Test will fail as there was not enough time to wait for message sweep.");
        }
        Assert.assertEquals(1, w.countMessagesInStorage());
        /*
         * make sure the second follower has what it's supposed to; the second
         * message
         */
        DefaultFollowerBaseTest.assertProperOrder(follower2.getMessages(), secondMessage);
        // terminate following, make sure all the messages are cleared
        w.unfollow(follower2);
        follower2 = null;
        System.gc();
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
            Assert.fail("Test will fail as there was not enough time to wait for message sweep.");
        }
        Assert.assertEquals(0, w.countMessagesInStorage());
    }

}
