package com.github.triceo.splitlog.api;

import java.io.File;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * Prepares an instance of {@link LogWatch}. Unless overriden by the user, the
 * instance will have the following properties:
 *
 * <dl>
 * <dt>Reads file from beginning?</dt>
 * <dd>Yes.</dd>
 * <dt>Closes file when the reading is finished?</dt>
 * <dd>No.</dd>
 * <dt>The default interval between each read</dt>
 * <dd>See {@link #DEFAULT_DELAY_BETWEEN_READS_IN_MILLISECONDS}.</dd>
 * <dt>The buffer size for reading</dt>
 * <dd>See {@link #DEFAULT_READ_BUFFER_SIZE_IN_BYTES}.</dd>
 * <dt>Default message capacity</dt>
 * <dd>{@link Integer#MAX_VALUE}, the maximum possible.</dd>
 * <dt>Interval between two sweeps for unreachable messages.</dt>
 * <dd>See {@link #DEFAULT_DELAY_BETWEEN_SWEEPS_IN_MILLISECONDS}.</dd>
 * <dt>Interval between requesting tailing and the actual start of tailing.</dt>
 * </dl>
 *
 * By default, the instance will store (and notify of) every message that has
 * passed the {@link #getGateCondition()} and not do so for all others.
 */
public abstract class LogWatchBuilder {

    public static final long DEFAULT_DELAY_BETWEEN_READS_IN_MILLISECONDS = 1000;
    public static final long DEFAULT_DELAY_BETWEEN_SWEEPS_IN_MILLISECONDS = 60 * 1000;
    public static final int DEFAULT_READ_BUFFER_SIZE_IN_BYTES = 4096;

    /**
     * Used to construct a {@link LogWatch} for a particular log file.
     *
     * @return Builder that is used to configure the new log watch instance
     *         before using.
     */
    public static LogWatchBuilder getDefault() {
        final ServiceLoader<LogWatchBuilder> ldr = ServiceLoader.load(LogWatchBuilder.class);
        if (!ldr.iterator().hasNext()) {
            throw new IllegalStateException("No LogWatchBuilder implementation registered.");
        }
        return ldr.iterator().next();
    }

    private static long getDelay(final int length, final TimeUnit unit) {
        if (length < 1) {
            throw new IllegalArgumentException("The length of time must be at least 1.");
        }
        switch (unit) {
            case NANOSECONDS:
                if (length < (1000 * 1000)) {
                    throw new IllegalArgumentException("The length of time must amount to at least 1 ms.");
                }
                break;
            case MICROSECONDS:
                if (length < 1000) {
                    throw new IllegalArgumentException("The length of time must amount to at least 1 ms.");
                }
                break;
            default:
                // every other unit is more than 1 ms by default
        }
        return unit.toMillis(length);
    }

    private int bufferSize = LogWatchBuilder.DEFAULT_READ_BUFFER_SIZE_IN_BYTES;
    private boolean closingBetweenReads;
    private long delayBetweenReads = LogWatchBuilder.DEFAULT_DELAY_BETWEEN_READS_IN_MILLISECONDS;
    private long delayBetweenSweeps = LogWatchBuilder.DEFAULT_DELAY_BETWEEN_SWEEPS_IN_MILLISECONDS;
    private File fileToWatch;
    private SimpleMessageCondition gateCondition;
    private int limitCapacityTo = Integer.MAX_VALUE;
    private boolean readingFromBeginning = true;
    private SimpleMessageCondition storageCondition;

    /**
     * Build the log watch with previously defined properties, or defaults where
     * not overriden. Such log watch will not start actually reading
     * {@link #getFileToWatch()} until after {@link LogWatch#startFollowing()}
     * or
     * {@link LogWatch#startConsuming(com.github.triceo.splitlog.api.MessageListener)}
     * .
     *
     * @return The newly built log watch.
     */
    public abstract LogWatch build();

    /**
     * Build the log watch with previously defined properties, or defaults where
     * not overriden, and immediately start listening for {@link Message}s.
     *
     * @return The follower that will receive the initial messages. The actual
     *         {@link LogWatch} can be retrieved by
     *         {@link Follower#getFollowed()}.
     */
    public abstract Follower buildFollowing();

    /**
     * Build the log watch with previously defined properties, or defaults where
     * not overriden, and immediately start listening for {@link Message}s
     *
     * @param splitter
     *            The splitter instance to use for the log watch instead of the
     *            default.
     * @return The follower that will receive the initial messages. The actual
     *         {@link LogWatch} can be retrieved by
     *         {@link Follower#getFollowed()}.
     */
    public abstract Follower buildFollowingWith(final TailSplitter splitter);

    /**
     * Build the log watch with previously defined properties, or defaults where
     * not overriden. Such log watch will not start actually reading
     * {@link #getFileToWatch()} until after {@link LogWatch#startFollowing()}
     * or
     * {@link LogWatch#startConsuming(com.github.triceo.splitlog.api.MessageListener)}
     * .
     *
     * @param splitter
     *            The splitter instance to use for the log watch instead of the
     *            default.
     * @return The newly built log watch. This log watch will not start tailing
     *         until after {@link LogWatch#startFollowing()} or
     *         {@link LogWatch#startConsuming(com.github.triceo.splitlog.api.MessageListener)}
     *         .
     */
    public abstract LogWatch buildWith(final TailSplitter splitter);

    /**
     * Change the default behavior of the future log watch to close the watched
     * file after each reading.
     *
     * @return This.
     */
    public LogWatchBuilder closingAfterReading() {
        this.closingBetweenReads = true;
        return this;
    }

    /**
     * Get the capacity of the future log watch.
     *
     * @return Maximum capacity in messages.
     */
    public int getCapacityLimit() {
        return this.limitCapacityTo;
    }

    /**
     * Get the delay between attempts to read from the watched file.
     *
     * @return In milliseconds.
     */
    public long getDelayBetweenReads() {
        return this.delayBetweenReads;
    }

    /**
     * Get the delay between attempts to sweep unreachable messages from memory.
     *
     * @return In milliseconds.
     */
    public long getDelayBetweenSweeps() {
        return this.delayBetweenSweeps;
    }

    /**
     * Get the file that the log watch will be watching.
     *
     * @return The file that will be watched by the future log watch.
     */
    public File getFileToWatch() {
        return this.fileToWatch;
    }

    /**
     * The condition that will be used for accepting a {@link Message} into
     * {@link LogWatch}.
     *
     * @return The condition.
     */
    public SimpleMessageCondition getGateCondition() {
        return this.gateCondition;
    }

    /**
     * Get the buffer size for the log watch.
     *
     * @return In bytes.
     */
    public int getReadingBufferSize() {
        return this.bufferSize;
    }

    /**
     * The condition that will be used for storing a {@link Message} within
     * {@link LogWatch}.
     *
     * @return The condition.
     */
    public SimpleMessageCondition getStorageCondition() {
        return this.storageCondition;
    }

    /**
     * Change the default behavior of the future log watch so that the existing
     * contents of the file is ignored and only the future additions to the file
     * are reported.
     *
     * @return This.
     */
    public LogWatchBuilder ignoringPreexistingContent() {
        this.readingFromBeginning = false;
        return this;
    }

    /**
     * @return Whether or not the file will be closed after it is read from.
     */
    public boolean isClosingBetweenReads() {
        return this.closingBetweenReads;
    }

    /**
     * @return True if the file will be read from the beginning, false if just
     *         the additions made post starting the log watch.
     */
    public boolean isReadingFromBeginning() {
        return this.readingFromBeginning;
    }

    /**
     * Limit capacity of the log watch to a given amount of messages.
     *
     * @param size
     *            Maximum amount of messages to store.
     * @return This.
     */
    public LogWatchBuilder limitCapacityTo(final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size of the memory store must be larger than zero.");
        }
        this.limitCapacityTo = size;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LogWatchBuilder [bufferSize=").append(this.bufferSize).append(", closingBetweenReads=")
        .append(this.closingBetweenReads).append(", delayBetweenReads=").append(this.delayBetweenReads)
        .append(", delayBetweenSweeps=").append(this.delayBetweenSweeps).append(", ");
        if (this.fileToWatch != null) {
            builder.append("fileToWatch=").append(this.fileToWatch).append(", ");
        }
        if (this.gateCondition != null) {
            builder.append("gateCondition=").append(this.gateCondition).append(", ");
        }
        builder.append("limitCapacityTo=").append(this.limitCapacityTo).append(", readingFromBeginning=")
        .append(this.readingFromBeginning).append(", ");
        if (this.storageCondition != null) {
            builder.append("storageCondition=").append(this.storageCondition);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Set the file that the future {@link LogWatch} will be tailing.
     *
     * @param f
     *            File to watch.
     * @return This.
     */
    public LogWatchBuilder watchingFile(final File f) {
        if (f == null) {
            throw new IllegalArgumentException("File can not be null.");
        }
        this.fileToWatch = f;
        return this;
    }

    /**
     * Specify the delay between attempts to read the file.
     *
     * @param length
     *            Length of time.
     * @param unit
     *            Unit of that length.
     * @return This.
     */
    public LogWatchBuilder withDelayBetweenReads(final int length, final TimeUnit unit) {
        this.delayBetweenReads = LogWatchBuilder.getDelay(length, unit);
        return this;
    }

    /**
     * Specify the delay between attempts to sweep the log watch from
     * unreachable messages.
     *
     * @param length
     *            Length of time.
     * @param unit
     *            Unit of that length.
     * @return This.
     */
    public LogWatchBuilder withDelayBetweenSweeps(final int length, final TimeUnit unit) {
        this.delayBetweenSweeps = LogWatchBuilder.getDelay(length, unit);
        return this;
    }

    /**
     * Only the {@link Message}s for which
     * {@link SimpleMessageCondition#accept(Message)} is true will be passed to
     * {@link Follower}s and other {@link MessageConsumer}s. Such
     * {@link Message}s will also never be stored. For the purposes of Splitlog,
     * if it fails this condition, it's as if it never existed.
     *
     * @param condition
     *            The condition.
     * @return This.
     */
    public LogWatchBuilder withGateCondition(final SimpleMessageCondition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Gate condition must not be null.");
        }
        this.gateCondition = condition;
        return this;
    }

    /**
     * Specify the buffer size that will be used for reading changes made to the
     * watched file.
     *
     * @param bufferSize
     *            In bytes.
     * @return This.
     */
    public LogWatchBuilder withReadingBufferSize(final int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Buffer size must be at least 1.");
        }
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * Only the messages for which
     * {@link SimpleMessageCondition#accept(Message)} is true will be stored
     * within the future {@link LogWatch}. However, {@link MessageConsumer}s
     * will be notified of them either way.
     *
     * The condition in question will only ever be called on {@link Message}s
     * that have already passed the {@link #getGateCondition()}.
     *
     * @param condition
     *            The condition.
     * @return This.
     */
    public LogWatchBuilder withStorageCondition(final SimpleMessageCondition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Storage acceptance condition must not be null.");
        }
        this.storageCondition = condition;
        return this;
    }

}
