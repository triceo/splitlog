package com.github.triceo.splitlog;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageAction;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.conditions.AllLogWatchMessagesAcceptingCondition;

public class ExpectationTest extends DefaultFollowerBaseTest {

    private static final int THREADS = 100;
    private static final int TOTAL_MESSAGES = 10;
    private ExecutorService es;

    @Before
    public void setUp() {
        this.es = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() {
        this.es.shutdownNow();
        try {
            this.es.awaitTermination(2, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            System.err.println("Executor service failed to terminate.");
        }
    }

    @Test
    public void testConcurrentExpectations() {
        final HashMap<Future<Message>, String> tasks = new HashMap<Future<Message>, String>(ExpectationTest.THREADS);
        final Random random = new Random();
        for (int i = 0; i < ExpectationTest.THREADS; i++) {
            // First thread always waits for the last message, the rest is
            // random.
            final String expectedValue = "<"
                    + ((i == 0) ? String.valueOf(ExpectationTest.TOTAL_MESSAGES - 1) : String.valueOf(random
                            .nextInt(ExpectationTest.TOTAL_MESSAGES))) + ">";
            final Follower f = this.getLogWatch().startFollowing();
            final Future<Message> task = f.expect(new MidDeliveryMessageCondition<LogWatch>() {

                @Override
                public boolean
                accept(final Message evaluate, final MessageDeliveryStatus status, final LogWatch source) {
                    return evaluate.getLines().get(0).endsWith(expectedValue);
                }

            });
            tasks.put(task, expectedValue);
        }
        for (int i = 0; i < ExpectationTest.TOTAL_MESSAGES; i++) {
            this.getWriter().writeNow("<" + String.valueOf(i) + ">");
        }
        for (final Future<Message> task : tasks.keySet()) {
            try {
                final Message accepted = task.get();
                Assertions.assertThat(accepted).as("Failed to accept message #" + tasks.get(task)).isNotNull();
            } catch (final Exception e) {
                Assertions.fail("Failed to accept message #" + tasks.get(task), e);
            }
        }
    }

    @Test
    public void testExpectationWithAction() {
        final long timeout = 5000; // how long
        final Follower f = this.getLogWatch().startFollowing();
        final long startTime = System.nanoTime();
        final Future<Message> future = f.expect(AllLogWatchMessagesAcceptingCondition.INSTANCE,
                new MessageAction<LogWatch>() {

            @Override
            public void execute(final Message message, final LogWatch source) {
                try {
                    // emulates some long-running action
                    Thread.sleep(timeout);
                } catch (final InterruptedException e) {
                    throw new IllegalStateException("Failed waiting.");
                }
            }
        });
        this.getWriter().writeNow("test");
        try {
            future.get(); // should not return before the long-running
            final long runTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            Assertions.assertThat(runTime).isGreaterThanOrEqualTo(timeout);
        } catch (final Exception e) {
            Assertions.fail("Failed waiting for message.", e);
        }
    }
}
