package com.github.triceo.splitlog;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

public class ThreadSafetyTest extends DefaultTailerBaseTest {

    public static final long TIMEOUT_MILLIS = 1000 * 10;

    @Test(timeout = TIMEOUT_MILLIS)
    public void testThreadSafety() throws InterruptedException, ExecutionException {
        ExecutorService e = Executors.newFixedThreadPool(2);
        e.execute(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    ThreadSafetyTest.this.getWriter().writeWithoutWaiting(UUID.randomUUID().toString());
                }
            }
        });
        final LogTailer tailer = this.getLogWatch().startTailing();
        Future<?> reader = e.submit(new Runnable() {

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
