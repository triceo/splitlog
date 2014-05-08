package com.github.triceo.splitlog.expectations;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageProducer;

abstract class AbstractExpectationManager<P extends MessageProducer<P>, C> implements MessageConsumer<P> {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private final Set<AbstractExpectation<C, P>> exchanges = new HashSet<AbstractExpectation<C, P>>();
    private boolean isStopped = false;

    protected abstract AbstractExpectation<C, P> createExpectation(final C condition);

    @Override
    public boolean isStopped() {
        return this.isStopped;
    }

    @Override
    public synchronized void
        messageReceived(final Message message, final MessageDeliveryStatus status, final P producer) {
        if (this.isStopped()) {
            throw new IllegalStateException("Already stopped.");
        }
        for (final AbstractExpectation<C, P> exchange : this.exchanges) {
            exchange.messageReceived(message, status, producer);
        }
    }

    public synchronized Future<Message> setExpectation(final C condition) {
        if (this.isStopped()) {
            throw new IllegalStateException("Already stopped.");
        }
        final AbstractExpectation<C, P> exchange = this.createExpectation(condition);
        this.exchanges.add(exchange);
        return AbstractExpectationManager.EXECUTOR.submit(exchange);
    }

    @Override
    public boolean stop() {
        if (this.isStopped()) {
            return false;
        }
        this.exchanges.clear();
        this.isStopped = true;
        return true;
    }

    protected synchronized boolean unsetExpectation(final AbstractExpectation<C, P> exchange) {
        return this.exchanges.remove(exchange);
    }

}
