package com.github.triceo.splitlog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LimitedMessageStoreTest extends DefaultTailerBaseTest {

    private static final int CAPACITY = 1;
    private static final int TIMEOUT_MILLIS = 10000;
    private ExecutorService es;

    @Before
    public void setUp() {
        this.es = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() {
        es.shutdownNow();
        try {
            es.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            System.err.println("Executor service failed to terminate.");
        }
    }

    @Override
    protected LogWatchBuilder getBuilder() {
        return super.getBuilder().limitCapacityTo(CAPACITY);
    }

    @Test(timeout = TIMEOUT_MILLIS)
    public void testLimitedMessageStore() throws InterruptedException, ExecutionException {
        es.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    getWriter().writeWithoutWaiting("test");
                }
            }
        });
        final LogTailer tailer = getLogWatch().startTailing();
        Future<?> reader = es.submit(new Runnable() {
            @Override
            public void run() {
                long maxMillis = TIMEOUT_MILLIS / 2;
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < maxMillis) {
                    tailer.getMessages();
                }
            }
        });
        reader.get();
    }
}
