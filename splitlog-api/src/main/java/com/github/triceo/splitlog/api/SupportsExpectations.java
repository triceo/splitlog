package com.github.triceo.splitlog.api;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

interface SupportsExpectations<S extends MessageProducer<S>, C> {

    /**
     * Will return a future that will only return when a message arrives that
     * makes the given condition return true.
     *
     * @param condition
     *            Condition that needs to be true for the future to unblock.
     * @return Null if the method unblocked due to some other reason.
     * @throws IllegalStateException
     *             When already {@link #isStopped()}.
     */
    Future<Message> expect(C condition);

    /**
     * Will return a future that will only return when a message arrives that
     * makes the given condition return true, at which point it asynchronously
     * executes a particular action. It will not return until that action has
     * finished executing.
     *
     * @param condition
     *            Condition that needs to be true for the future to unblock.
     * @param action
     *            Action to execute when the condition becomes true.
     * @return Null if the method unblocked due to some other reason.
     * @throws IllegalStateException
     *             When already {@link #isStopped()}.
     */
    Future<Message> expect(C condition, MessageAction<S> action);

    /**
     * Will block until a message arrives, for which the condition is true.
     *
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @return Null if the method unblocked due to some other reason.
     * @throws IllegalStateException
     *             When already {@link #isStopped()}.
     */
    @Deprecated
    Message waitFor(C condition);

    /**
     * Will block until a message arrives, for which the condition is true. If
     * none arrives before the timeout, it unblocks anyway.
     *
     * @param condition
     *            Condition that needs to be true for the method to unblock.
     * @param timeout
     *            Time before forcibly aborting.
     * @param unit
     *            Unit of time.
     * @return Null if the method unblocked due to some other reason.
     * @throws IllegalStateException
     *             When already {@link #isStopped()}.
     */
    @Deprecated
    Message waitFor(C condition, long timeout, TimeUnit unit);

}
