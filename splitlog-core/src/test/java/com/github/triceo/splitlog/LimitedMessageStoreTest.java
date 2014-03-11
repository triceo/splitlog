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

/**
 * See <a href="https://github.com/triceo/splitlog/pull/15">Splitlog Pull Request 15</a>.
 */
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
        this.es.shutdownNow();
        try {
            this.es.awaitTermination(2, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            System.err.println("Executor service failed to terminate.");
        }
    }

    @Override
    protected LogWatchBuilder getBuilder() {
        return super.getBuilder().limitCapacityTo(LimitedMessageStoreTest.CAPACITY);
    }

    @Test(timeout = LimitedMessageStoreTest.TIMEOUT_MILLIS)
    public void testLimitedMessageStore() throws InterruptedException, ExecutionException {
        this.es.execute(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    LimitedMessageStoreTest.this.getWriter().writeWithoutWaiting(UUID.randomUUID().toString());
                }
            }
        });
        final LogTailer tailer = this.getLogWatch().startTailing();
        final Future<?> reader = this.es.submit(new Runnable() {
            @Override
            public void run() {
                final long maxMillis = LimitedMessageStoreTest.TIMEOUT_MILLIS / 2;
                final long start = System.currentTimeMillis();
                while ((System.currentTimeMillis() - start) < maxMillis) {
                    tailer.getMessages();
                }
            }
        });
        reader.get();
    }
}
