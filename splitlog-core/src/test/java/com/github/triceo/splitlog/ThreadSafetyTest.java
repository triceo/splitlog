package com.github.triceo.splitlog;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ThreadSafetyTest extends DefaultTailerBaseTest {

    private static final long TIMEOUT_MILLIS = 10000;
    private ExecutorService es;

    @Before
    public void setUp() {
        this.es = Executors.newFixedThreadPool(2);
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

    @Test(timeout = TIMEOUT_MILLIS)
    public void testThreadSafety() throws InterruptedException, ExecutionException {
        this.es.execute(new Runnable() {

            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    ThreadSafetyTest.this.getWriter().writeWithoutWaiting(UUID.randomUUID().toString());
                }
            }
        });
        final LogTailer tailer = this.getLogWatch().startTailing();
        Future<?> reader = this.es.submit(new Runnable() {

            @Override
            public void run() {
                int size = 0;
                long maxMillis = TIMEOUT_MILLIS / 2;
                long start = System.currentTimeMillis();
                while (size < 1000 && System.currentTimeMillis() - start < maxMillis) {
                    size = tailer.getMessages().size();
                    System.out.println("Messages: " + size);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.err.println("Interrupted.");
                    }
                }
            }
        });
        reader.get();
    }
}
