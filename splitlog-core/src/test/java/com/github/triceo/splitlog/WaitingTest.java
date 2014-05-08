package com.github.triceo.splitlog;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutionException;
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
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;

public class WaitingTest extends DefaultFollowerBaseTest {

    private static final int THREADS = 100;
    private static final int TIMEOUT_MILLIS = 10000;
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

    @Test(timeout = WaitingTest.TIMEOUT_MILLIS)
    public void testConcurrentWaitingForMessages() throws InterruptedException, ExecutionException {
        final HashMap<Future<Message>, String> tasks = new HashMap<Future<Message>, String>(WaitingTest.THREADS);
        final Random random = new Random();
        for (int i = 0; i < WaitingTest.THREADS; i++) {
            // First thread always waits for the last message, the rest is
            // random.
            final String expectedValue = "<"
                    + ((i == 0) ? String.valueOf(WaitingTest.TOTAL_MESSAGES - 1) : String.valueOf(random
                            .nextInt(WaitingTest.TOTAL_MESSAGES))) + ">";
            final Follower f = this.getLogWatch().startFollowing();
            final Future<Message> task = f.expect(new MidDeliveryMessageCondition<LogWatch>() {

                @Override
                public boolean accept(final Message evaluate, final MessageDeliveryStatus status,
                    final LogWatch source) {
                    return evaluate.getLines().get(0).endsWith(expectedValue);
                }

            });
            tasks.put(task, expectedValue);
        }
        for (int i = 0; i < WaitingTest.TOTAL_MESSAGES; i++) {
            this.getWriter().writeNow("<" + String.valueOf(i) + ">");
        }
        for (final Future<Message> task : tasks.keySet()) {
            final Message accepted = task.get();
            Assertions.assertThat(accepted).as("Failed to accept message #" + tasks.get(task)).isNotNull();
        }
        System.out.println("All messages collected.");
    }
}
