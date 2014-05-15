package com.github.triceo.splitlog;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

@RunWith(Parameterized.class)
public class ReferenceBasedDiscardingTest extends DefaultFollowerBaseTest {

    public ReferenceBasedDiscardingTest(final LogWatchBuilder builder) {
        super(builder.withDelayBetweenSweeps(1, TimeUnit.SECONDS));
    }

    @Before
    public void disableLogging() {
        /*
         * logging, when async, may store messages and their attributes for
         * future writing. this will keep the followers from being GC'd and will
         * break this test.
         */
        SplitlogLoggerFactory.silenceLogging();
    }

    @After
    public void enableLogging() {
        SplitlogLoggerFactory.resetLoggingToDefaultState();
    }

    @Test
    public void test() {
        final DefaultLogWatch w = (DefaultLogWatch) this.getLogWatch();
        Follower follower = w.startFollowing();
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
        // start second follower; this one will only track second+ messages
        Follower follower2 = w.startFollowing();
        // send third message, receive second
        final String thirdMessage = "check3";
        LogWriter.write(follower2, thirdMessage);
        Assertions.assertThat(w.countMessagesInStorage()).isEqualTo(2);
        /*
         * remove all references to the first follower; the first message now
         * has no followers available and can be GC'd
         */
        w.stopFollowing(follower);
        follower = null;
        System.gc();
        final long delay = this.getBuilder().getDelayBetweenSweeps() + 500;
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
            Assertions.fail("Test will fail as there was not enough time to wait for message sweep.");
        }
        Assertions.assertThat(w.countMessagesInStorage()).isEqualTo(1);
        DefaultFollowerBaseTest.assertProperOrder(follower2.getMessages(), secondMessage);
        // terminate following, make sure all the messages are cleared
        w.stopFollowing(follower2);
        follower2 = null;
        System.gc();
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
            Assertions.fail("Test will fail as there was not enough time to wait for message sweep.");
        }
        Assertions.assertThat(w.countMessagesInStorage()).isEqualTo(0);
    }

}
