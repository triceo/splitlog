package com.github.triceo.splitlog.expectations;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageAction;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.util.SplitlogThreadFactory;

/**
 * Waits until a {@link Message} arrives that makes a particular condition true.
 *
 * @param <C>
 *            Type of the condition to make true.
 * @param <S>
 *            The source to receive the messages from.
 */
abstract class AbstractExpectation<C, S extends MessageProducer<S>> implements MessageListener<S>, Callable<Message> {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new SplitlogThreadFactory("actions"));

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(AbstractExpectation.class);

    private final MessageAction<S> action;
    private Future<Void> actionFuture;
    private final C blockingCondition;
    private final AtomicBoolean conditionPassed = new AtomicBoolean(false);
    private final CountDownLatch latch = new CountDownLatch(1);
    /**
     * Whether or not {@link #call()} is in progress.
     */
    private final AbstractExpectationManager<S, C> manager;
    private Message stash = null;

    protected AbstractExpectation(final AbstractExpectationManager<S, C> manager, final C condition,
        final MessageAction<S> action) {
        if (manager == null) {
            throw new IllegalArgumentException("Must provide manager.");
        } else if (condition == null) {
            throw new IllegalArgumentException("Must provide condition.");
        }
        this.action = action;
        this.manager = manager;
        this.blockingCondition = condition;
    }

    /**
     * Will start the wait for the condition to become true.
     */
    @Override
    public Message call() throws InterruptedException {
        AbstractExpectation.LOGGER.info("Thread blocked waiting for message to pass condition {}.",
                this.getBlockingCondition());
        try {
            this.latch.await();
            this.manager.unsetExpectation(this);
            AbstractExpectation.LOGGER.info("Condition passed.");
            if (this.actionFuture != null) {
                this.waitUntilActionComplete();
            }
            AbstractExpectation.LOGGER.info("Expectation processing passed.");
            return this.stash;
        } finally {
            this.manager.unsetExpectation(this); // in case await() throws
            this.actionFuture = null;
            AbstractExpectation.LOGGER.info("Thread unblocked.");
        }
    }

    protected C getBlockingCondition() {
        return this.blockingCondition;
    }

    /**
     * Whether or not the given condition is true.
     *
     * @param msg
     *            Message that's just arrived.
     * @param status
     *            Status of the message.
     * @param source
     *            Where the message comes from.
     * @return True if the thread can unblock.
     */
    protected abstract boolean isAccepted(final Message msg, final MessageDeliveryStatus status, final S source);

    @Override
    public synchronized void messageReceived(final Message msg, final MessageDeliveryStatus status, final S source) {
        AbstractExpectation.LOGGER.info("Notified of message '{}' in state {} from {}.", msg, status, source);
        // check if the user code accepts the message
        if (!this.isAccepted(msg, status, source)) {
            return;
        } else if (this.conditionPassed.getAndSet(true)) {
            AbstractExpectation.LOGGER.debug("Not continuing with processing as condition already triggered once.");
            return;
        }
        this.manager.unsetExpectation(this); // don't notify again
        AbstractExpectation.LOGGER.debug("Condition passed by message '{}' in state {} from {}.", msg, status, source);
        this.stash = msg;
        if (this.action != null) {
            this.actionFuture = AbstractExpectation.EXECUTOR.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    AbstractExpectation.this.action.execute(msg, source);
                    return null;
                }

            });
        }
        this.latch.countDown(); // unblock the other thread
    }

    private void waitUntilActionComplete() {
        try {
            AbstractExpectation.LOGGER.debug("Waiting for the user action to finish.");
            this.actionFuture.get();
            AbstractExpectation.LOGGER.debug("User action completed.");
        } catch (final Exception e) {
            AbstractExpectation.LOGGER.warn("User action ended abruptly.", e);
        }
    }

}
