package com.github.triceo.splitlog;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.conditions.AllLogWatchMessagesAcceptingCondition;

@RunWith(Parameterized.class)
public class FailingExpectationTest extends DefaultFollowerBaseTest {

    public FailingExpectationTest(final LogWatchBuilder builder) {
        super(builder);
    }

    @Test
    public void testExpectationAfterPreviousFailed() {
        final Follower follower = this.getLogWatch().startFollowing();
        // this call will fail, since we're not writing anything
        try {
            follower.expect(AllLogWatchMessagesAcceptingCondition.INSTANCE).get(1, TimeUnit.SECONDS);
            Assertions.fail("No message should've been received.");
        } catch (final InterruptedException e) {
            Assertions.fail("Message wait interrupted.", e);
        } catch (final ExecutionException e) {
            Assertions.fail("Message wait interrupted due to a problem.", e);
        } catch (final TimeoutException e) {
            // this is expected
        }
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
