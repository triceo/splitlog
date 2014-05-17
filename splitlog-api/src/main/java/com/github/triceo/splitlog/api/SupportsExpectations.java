package com.github.triceo.splitlog.api;

import java.util.concurrent.Future;

interface SupportsExpectations<S extends MessageProducer<S>, C> {

    /**
     * Will return a future that will only return when a message arrives that
     * makes the given condition return true.
     *
     * @param condition
     *            Condition that needs to be true for the future to unblock.
     * @return Null if the method unblocked due to some other reason.
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
     */
    Future<Message> expect(C condition, MessageAction<S> action);

}
