package com.github.triceo.splitlog;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.LogWatchBuilder;
import com.github.triceo.splitlog.api.TailSplitter;
import com.github.triceo.splitlog.conditions.AllFollowerMessagesAcceptingCondition;
import com.github.triceo.splitlog.conditions.SplitlogMessagesRejectingCondition;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;
import com.github.triceo.splitlog.splitters.SimpleTailSplitter;

/**
 * Prepares an instance of {@link LogWatch}. Unless overriden by the user, the
 * instance will have the following properties:
 *
 * <dl>
 * <dt>Default gating condition</dt>
 * <dd>{@link SplitlogMessagesRejectingCondition}.</dd>
 * <dt>Default storage condition</dt>
 * <dd>{@link AllFollowerMessagesAcceptingCondition}.</dd>
 * </dl>
 *
 */
final public class DefaultLogWatchBuilder extends LogWatchBuilder {

    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(DefaultLogWatchBuilder.class);

    public DefaultLogWatchBuilder() {
        super();
        this.withGateCondition(SplitlogMessagesRejectingCondition.INSTANCE);
        this.withStorageCondition(AllFollowerMessagesAcceptingCondition.INSTANCE);
    }

    @Override
    public LogWatch build() {
        return this.buildWith(new SimpleTailSplitter());
    }

    @Override
    public Follower buildFollowing() {
        return this.buildFollowingWith(new SimpleTailSplitter());
    }

    @Override
    public Follower buildFollowingWith(final TailSplitter splitter) {
        if (splitter == null) {
            throw new IllegalArgumentException("A splitter must be provided.");
        }
        return this.buildWith(splitter).startFollowing();
    }

    @Override
    public LogWatch buildWith(final TailSplitter splitter) {
        if (splitter == null) {
            throw new IllegalArgumentException("A splitter must be provided.");
        } else if ((splitter instanceof SimpleTailSplitter)
                && (this.getGateCondition() instanceof SplitlogMessagesRejectingCondition)) {
            DefaultLogWatchBuilder.LOGGER
                    .warn("Using default TailSplitter with default gating condition. All messages will pass through gate, as the TailSplitter will not provide all the necessary information.");
        }
        return new DefaultLogWatch(this, splitter);
    }

}
