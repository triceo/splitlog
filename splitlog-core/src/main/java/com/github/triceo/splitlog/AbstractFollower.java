package com.github.triceo.splitlog;

import java.io.OutputStream;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.github.triceo.splitlog.api.CommonFollower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;
import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageFormatter;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageProducer;
import com.github.triceo.splitlog.api.MidDeliveryMessageCondition;
import com.github.triceo.splitlog.api.SimpleMessageCondition;
import com.github.triceo.splitlog.conditions.AllLogWatchMessagesAcceptingCondition;
import com.github.triceo.splitlog.ordering.OriginalOrderingMessageComprator;

/**
 * Internal API for a log follower that, on top of the public API, provides ways
 * for {@link LogWatch} of notifying the follower of new messages. Every
 * follower implementation, such as {@link NonStoringFollower}, needs to extend
 * this class.
 *
 * Will use {@link #getDefaultFormatter()} as default message formatter. Will
 * use {@value #DEFAULT_CONDITION} as a default in getMessages() and write()
 * methods. Will use {@link #DEFAULT_COMPARATOR} as a default order for the
 * messages.
 */
abstract class AbstractFollower<P extends MessageProducer<P>, C extends MessageProducer<C>> implements
CommonFollower<P, C>, ConsumerRegistrar<P> {

    private static final MessageComparator DEFAULT_COMPARATOR = OriginalOrderingMessageComprator.INSTANCE;
    private static final SimpleMessageCondition DEFAULT_CONDITION = AllLogWatchMessagesAcceptingCondition.INSTANCE;
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private final MessageExchange<C> exchange = new MessageExchange<C>();

    private final long uniqueId = AbstractFollower.ID_GENERATOR.getAndIncrement();

    @Override
    public int countConsumers() {
        return this.getConsumerManager().countConsumers();
    }

    @Override
    public int countMetrics() {
        return this.getConsumerManager().countMetrics();
    }

    protected abstract ConsumerManager<P> getConsumerManager();

    /**
     * Provide the default formatter for messages in this follower.
     *
     * @return Formatter to use on messages.
     */
    protected abstract MessageFormatter getDefaultFormatter();

    protected MessageExchange<C> getExchange() {
        return this.exchange;
    }

    @Override
    public SortedSet<Message> getMessages() {
        return this.getMessages(AbstractFollower.DEFAULT_COMPARATOR);
    }

    @Override
    public SortedSet<Message> getMessages(final MessageComparator order) {
        return this.getMessages(AbstractFollower.DEFAULT_CONDITION, order);
    }

    @Override
    public SortedSet<Message> getMessages(final SimpleMessageCondition condition) {
        return this.getMessages(condition, AbstractFollower.DEFAULT_COMPARATOR);
    }

    @Override
    public MessageMetric<? extends Number, P> getMetric(final String id) {
        return this.getConsumerManager().getMetric(id);
    }

    @Override
    public String getMetricId(final MessageMetric<? extends Number, P> measure) {
        return this.getConsumerManager().getMetricId(measure);
    }

    public long getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public boolean isConsuming(final MessageConsumer<P> consumer) {
        return this.getConsumerManager().isConsuming(consumer);
    }

    @Override
    public boolean isMeasuring(final MessageMetric<? extends Number, P> metric) {
        return this.getConsumerManager().isMeasuring(metric);
    }

    @Override
    public boolean isMeasuring(final String id) {
        return this.getConsumerManager().isMeasuring(id);
    }

    @Override
    public void registerConsumer(final MessageConsumer<P> consumer) {
        this.getConsumerManager().registerConsumer(consumer);
    }

    @Override
    public MessageConsumer<P> startConsuming(final MessageListener<P> listener) {
        return this.getConsumerManager().startConsuming(listener);
    }

    @Override
    public <T extends Number> MessageMetric<T, P> startMeasuring(final MessageMeasure<T, P> measure, final String id) {
        return this.startMeasuring(measure, id, true);
    }

    protected synchronized <T extends Number> MessageMetric<T, P> startMeasuring(final MessageMeasure<T, P> measure,
            final String id, final boolean checkIfFollowing) {
        if (checkIfFollowing && this.isStopped()) {
            throw new IllegalStateException("Cannot start measurement as the follower is no longer active.");
        }
        return this.getConsumerManager().startMeasuring(measure, id);
    }

    @Override
    public boolean stopConsuming(final MessageConsumer<P> consumer) {
        return this.getConsumerManager().stopConsuming(consumer);
    }

    @Override
    public boolean stopMeasuring(final MessageMetric<? extends Number, P> metric) {
        return this.getConsumerManager().stopMeasuring(metric);
    }

    @Override
    public boolean stopMeasuring(final String id) {
        return this.getConsumerManager().stopMeasuring(id);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MidDeliveryMessageCondition<C> condition) {
        return this.exchange.waitForMessage(condition, -1, TimeUnit.NANOSECONDS);
    }

    /**
     * Will throw an exception if any other thread tries to specify a wait on
     * the instance while another thread is already waiting.
     */
    @Override
    public Message waitFor(final MidDeliveryMessageCondition<C> condition, final long timeout, final TimeUnit unit) {
        if (timeout < 1) {
            throw new IllegalArgumentException("Waiting time must be great than 0, but was: " + timeout + " " + unit);
        }
        return this.exchange.waitForMessage(condition, timeout, unit);
    }

    @Override
    public boolean write(final OutputStream stream) {
        return this.write(stream, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final MessageComparator order) {
        return this.write(stream, order, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final MessageComparator order, final MessageFormatter formatter) {
        return this.write(stream, AbstractFollower.DEFAULT_CONDITION, order, formatter);
    }

    @Override
    public boolean write(final OutputStream stream, final MessageFormatter formatter) {
        return this.write(stream, AbstractFollower.DEFAULT_CONDITION, formatter);
    }

    @Override
    public boolean write(final OutputStream stream, final SimpleMessageCondition condition) {
        return this.write(stream, condition, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final SimpleMessageCondition condition,
        final MessageComparator order) {
        return this.write(stream, condition, order, this.getDefaultFormatter());
    }

    @Override
    public boolean write(final OutputStream stream, final SimpleMessageCondition condition,
        final MessageFormatter formatter) {
        return this.write(stream, condition, AbstractFollower.DEFAULT_COMPARATOR, formatter);
    }

}
