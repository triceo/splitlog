package com.github.triceo.splitlog;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.MessageDeliveryCondition;

final class MessageExchange {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageExchange.class);

    private MessageDeliveryCondition messageBlockingCondition = null;
    private final Exchanger<Message> messageExchanger = new Exchanger<Message>();

    public void notifyOfMessage(final Message msg, final MessageDeliveryStatus status,
        final MessageDeliveryNotificationSource source) {
        if (this.messageBlockingCondition == null) {
            // this does nothing with the message
            return;
        }
        // check if the user code accepts the message
        if (source instanceof LogWatch) {
            if (!this.messageBlockingCondition.accept(msg, status, (LogWatch) source)) {
                return;
            }
        } else if (source instanceof AbstractLogWatchFollower) {
            if (!this.messageBlockingCondition.accept(msg, status, (Follower) source)) {
                return;
            }
        } else {
            throw new IllegalStateException(source + " is not a valid message notification source.");
        }
        this.messageBlockingCondition = null;
        try {
            this.messageExchanger.exchange(msg);
        } catch (final InterruptedException e) {
            MessageExchange.LOGGER
                    .warn("Notifying follower {} of message {} in state {} failed.", this, msg, status, e);
        }
    }

    /*
     * The method is synchronized, therefore no thread will be able to overwrite
     * an already set condition. That condition will later be unset by the
     * notify*() method calls from the tailing thread.
     */
    public synchronized Message waitForMessage(final MessageDeliveryCondition condition, final long timeout,
        final TimeUnit unit) {
        this.messageBlockingCondition = condition;
        try {
            if (timeout < 0) {
                return this.messageExchanger.exchange(null);
            } else {
                return this.messageExchanger.exchange(null, timeout, unit);
            }
        } catch (final TimeoutException e) {
            return null;
        } catch (final InterruptedException e) {
            return null;
        } finally { // just in case
            this.messageBlockingCondition = null;
        }
    }

}
