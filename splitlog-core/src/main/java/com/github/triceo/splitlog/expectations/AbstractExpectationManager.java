package com.github.triceo.splitlog.expectations;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageAction;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.util.LogUtil;
import com.github.triceo.splitlog.util.LogUtil.Level;
import com.github.triceo.splitlog.util.SplitlogThreadFactory;

/**
 * Tracks expectations that are currently active for a given message consumer.
 *
 * @param <P>
 *            The source for the messages.
 * @param <C>
 *            The type of condition that the expectations accept.
 */
abstract class AbstractExpectationManager<P extends MessageProducer<P>, C> implements MessageConsumer<P> {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new SplitlogThreadFactory(
            "expectations"));
    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(AbstractExpectationManager.class);
    private final ConcurrentMap<AbstractExpectation<C, P>, Future<Message>> expectations = new ConcurrentHashMap<AbstractExpectation<C, P>, Future<Message>>();

    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    protected abstract AbstractExpectation<C, P> createExpectation(final C condition, final MessageAction<P> action);

    @Override
    public synchronized boolean isStopped() {
        return this.isStopped.get();
    }

    @Override
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status, final P producer) {
        if (this.isStopped()) {
            throw new IllegalStateException("Already stopped.");
        }
        LogUtil.newMessage(AbstractExpectationManager.LOGGER, Level.INFO, "New message received:", msg, status,
                producer, this);
        for (final AbstractExpectation<C, P> exchange : this.expectations.keySet()) {
            exchange.messageReceived(msg, status, producer);
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
        final AbstractExpectation<C, P> expectation = this.createExpectation(condition, action);
        final Future<Message> future = AbstractExpectationManager.EXECUTOR.submit(expectation);
        this.expectations.put(expectation, future);
        AbstractExpectationManager.LOGGER.info("Registered expectation {} with action {}.", expectation, action);
        return future;
    }

    @Override
    public synchronized boolean stop() {
        if (!this.isStopped.compareAndSet(false, true)) {
            // already stopped
            return false;
        }
        for (final Future<Message> future : this.expectations.values()) {
            future.cancel(true);
        }
        this.expectations.clear();
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
        if (this.expectations.containsKey(expectation)) {
            this.expectations.remove(expectation);
            AbstractExpectationManager.LOGGER.info("Unregistered expectation {}.", expectation);
            return true;
        }
        return false;
    }

}
