package com.github.triceo.splitlog;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;

public class WaitingTest extends DefaultTailerBaseTest {

    private static final int TIMEOUT_MILLIS = 10000;
    private static final int TOTAL_MESSAGES = 10;
    private static final int THREADS = 100;
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
        HashMap<Future<Message>, String> tasks = new HashMap<Future<Message>, String>(THREADS);
        Random random = new Random();
        for (int i = 0; i < THREADS; i++) {
            // First thread always waits for the last message, the rest is random.
            final String expectedValue = "<" + ((i == 0)
                    ? String.valueOf(TOTAL_MESSAGES - 1)
                    : String.valueOf(random.nextInt(TOTAL_MESSAGES))) + ">";
            Future<Message> task = this.es.submit(new Callable<Message>() {
                @Override
                public Message call() {
                    final LogTailer tailer = WaitingTest.this.getLogWatch().startTailing();
                    return tailer.waitFor(new MessageDeliveryCondition() {
                        @Override
                        public boolean accept(Message evaluate, MessageDeliveryStatus status) {
                            return evaluate.toString().trim().endsWith(expectedValue);
                        }
                    }, TIMEOUT_MILLIS / 2, TimeUnit.MILLISECONDS);
                }
            });
            tasks.put(task, expectedValue);
        }
        for (int i = 0; i < TOTAL_MESSAGES; i++) {
            this.getWriter().writeWithoutWaiting("<" + String.valueOf(i) + ">");
        }
        for (Future<Message> task : tasks.keySet()) {
            Message accepted = task.get();
            Assert.assertNotNull("Failed to accept message #" + tasks.get(task), accepted);
        }
        System.out.println("All messages collected.");
    }
}
