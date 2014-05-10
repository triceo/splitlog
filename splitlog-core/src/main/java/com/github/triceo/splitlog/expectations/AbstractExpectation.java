package com.github.triceo.splitlog.expectations;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageAction;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

/**
 * Waits until a {@link Message} arrives that makes a particular condition true.
 *
 * FIXME this is some very ugly code
 *
 * @param <C>
 *            Type of the condition to make true.
 * @param <S>
 *            The source to receive the messages from.
 */
abstract class AbstractExpectation<C, S extends MessageProducer<S>> implements MessageListener<S>, Callable<Message> {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {

        private final ThreadGroup group = new ThreadGroup("actions");
        private final AtomicLong nextId = new AtomicLong(0);

        @Override
        public Thread newThread(final Runnable r) {
            return new Thread(this.group, r, this.group.getName() + "-" + this.nextId.incrementAndGet());
        }

    });

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(AbstractExpectation.class);

    private final MessageAction<S> action;
    private Future<Void> actionFuture;
    private final C blockingCondition;
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
    public Message call() {
        if (this.latch.getCount() == 0) {
            throw new IllegalStateException("Expectation already triggered.");
        } else if (this.stash != null) {
            /*
             * in case the condition has been triggered before this has been
             * called, we retrieve the message from the stash.
             */
            final Message msg = this.stash;
            this.stash = null;
            return msg;
        }
        try {
            AbstractExpectation.LOGGER.info("Thread blocked waiting for message to pass condition {}.",
                    this.getBlockingCondition());
            this.latch.await();
            this.manager.unsetExpectation(this); // don't notify again
            AbstractExpectation.LOGGER.info("Thread unblocked.");
            if (this.actionFuture != null) {
                AbstractExpectation.LOGGER.info("Executing user action.");
                try {
                    // wait for the future action to finish
                    this.actionFuture.get();
                } catch (final Exception e) {
                    AbstractExpectation.LOGGER.warn("User action ended abruptly.", e);
                }
            }
            return this.stash;
        } catch (final InterruptedException e) {
            return null;
        } finally {
            this.manager.unsetExpectation(this); // in case await() throws
            AbstractExpectation.LOGGER.info("Expectation processing finished.");
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
        }
        AbstractExpectation.LOGGER.debug("Condition passed by message '{}' in state {} from {}.", msg, status, source);
        if (this.stash == null) {
            /*
             * if call() hasn't been called yet, and the condition has already
             * triggered, stash the message for retrieval when call() is
             * actually called.
             */
            AbstractExpectation.LOGGER.debug("Stashing the message.");
            this.stash = msg;
            this.latch.countDown();
            if (this.action == null) {
                return;
            }
            try {
                this.actionFuture = AbstractExpectation.EXECUTOR.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        AbstractExpectation.this.action.execute(msg, source);
                        return null;
                    }

                });
            } catch (final Throwable t) {
                AbstractExpectation.LOGGER.info("Caught Throwable while executing user action {}.", this.action, t);
            }
        } else {
            AbstractExpectation.LOGGER.debug("Message already stashed.");
        }
    }

}
