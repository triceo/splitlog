package com.github.triceo.splitlog;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageSource;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;

final class MessageExchange {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageExchange.class);

    private MidDeliveryMessageCondition messageBlockingCondition = null;
    private final Exchanger<Message> messageExchanger = new Exchanger<Message>();

    public void notifyOfMessage(final Message msg, final MessageDeliveryStatus status, final MessageSource source) {
        MessageExchange.LOGGER.info("Notified of message '{}' in state {} from {}.", msg, status, source);
        if (this.messageBlockingCondition == null) {
            MessageExchange.LOGGER.debug("Not waiting for message '{}' in state {} from {}.", msg, status, source);
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
        MessageExchange.LOGGER.debug("Accepted message '{}' in state {} from {}.", msg, status, source);
        this.messageBlockingCondition = null;
        try {
            this.messageExchanger.exchange(msg);
        } catch (final InterruptedException e) {
            MessageExchange.LOGGER.warn("Failed to notify Follower of message '{}' in state {} from {}.", msg, status,
                    source, e);
        }
    }

    /*
     * The method is synchronized, therefore no thread will be able to overwrite
     * an already set condition. That condition will later be unset by the
     * notify*() method calls from the tailing thread.
     */
    public synchronized Message waitForMessage(final MidDeliveryMessageCondition condition, final long timeout,
        final TimeUnit unit) {
        this.messageBlockingCondition = condition;
        try {
            MessageExchange.LOGGER.info("Thread blocked waiting for message to pass condition {}.", condition);
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
            MessageExchange.LOGGER.info("Thread unblocked.");
            this.messageBlockingCondition = null;
        }
    }

}
