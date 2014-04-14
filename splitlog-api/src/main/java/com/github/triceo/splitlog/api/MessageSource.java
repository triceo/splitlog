package com.github.triceo.splitlog.api;

/**
 * A tagging interface to mark classes that are allowed to send message delivery
 * notifications to {@link CommonFollower}s.
 *
 * @param <S>
 *            The type to send out {@link Message} notifications. Typically it
 *            is the implementing type.
 */
public interface MessageSource<S extends MessageSource<S>> extends MessageMetricProducer<S> {

}
