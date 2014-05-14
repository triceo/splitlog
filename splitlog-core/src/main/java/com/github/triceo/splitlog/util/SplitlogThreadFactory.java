package com.github.triceo.splitlog.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class SplitlogThreadFactory implements ThreadFactory {

    private final AtomicLong nextId = new AtomicLong(0);

    private final ThreadGroup threadGroup;
    private final String threadGroupName;

    public SplitlogThreadFactory(final String threadGroupName) {
        this.threadGroupName = threadGroupName;
        this.threadGroup = new ThreadGroup(threadGroupName);
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Thread t = new Thread(this.threadGroup, r, this.newThreadName());
        t.setDaemon(true);
        return t;
    }

    private String newThreadName() {
        return this.threadGroupName + "-" + this.nextId.incrementAndGet();
    }

}
