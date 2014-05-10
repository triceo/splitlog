package com.github.triceo.splitlog.expectations;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageAction;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageProducer;

/**
 * Tracks expectations that are currently active for a given message consumer.
 *
 * @param <P>
 *            The source for the messages.
 * @param <C>
 *            The type of condition that the expectations accept.
 */
abstract class AbstractExpectationManager<P extends MessageProducer<P>, C> implements MessageConsumer<P> {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {

        private final AtomicLong ID_GENERATOR = new AtomicLong(0);

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(r, "expectations-" + this.ID_GENERATOR.incrementAndGet());
        }

    });
    private final Set<AbstractExpectation<C, P>> exchanges = new HashSet<AbstractExpectation<C, P>>();
    private boolean isStopped = false;

    protected abstract AbstractExpectation<C, P> createExpectation(final C condition, final MessageAction<P> action);

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

    /**
     * The resulting future will only return after such a message is received
     * that makes the condition true.
     *
     * @param condition
     *            Condition to be true.
     * @return The future.
     */
    public synchronized Future<Message> setExpectation(final C condition) {
        return this.setExpectation(condition, null);
    }

    /**
     * The resulting future will only return after such a message is received
     * that makes the condition true.
     *
     * @param condition
     *            Condition to be true.
     * @return The future.
     */
    public synchronized Future<Message> setExpectation(final C condition, final MessageAction<P> action) {
        if (this.isStopped()) {
            throw new IllegalStateException("Already stopped.");
        }
        final AbstractExpectation<C, P> exchange = this.createExpectation(condition, action);
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

    /**
     * Stop tracking this expectation. Calls from the internal code only.
     *
     * @param expectation
     *            The expectation to stop.
     * @return If stopped, false if stopped already.
     */
    protected synchronized boolean unsetExpectation(final AbstractExpectation<C, P> expectation) {
        return this.exchanges.remove(expectation);
    }

}
