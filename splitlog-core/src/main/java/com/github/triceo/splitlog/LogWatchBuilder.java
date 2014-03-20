package com.github.triceo.splitlog;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.github.triceo.splitlog.conditions.AllMessagesAcceptingCondition;
import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.splitters.SimpleTailSplitter;
import com.github.triceo.splitlog.splitters.TailSplitter;

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
 * <dt>Default tail splitter</dt>
 * <dd>See {@link SimpleTailSplitter}</dd>
 * <dt>Default message capacity</dt>
 * <dd>{@link Integer#MAX_VALUE}, the maximum possible. See {@link MessageStore}
 * <dt>Interval between two sweeps for unreachable messages.</dt>
 * <dd>See {@link #DEFAULT_DELAY_BETWEEN_SWEEPS_IN_MILLISECONDS}.</dd>
 * </dl>
 * 
 * By default, the instance will allow every message through.
 */
public class LogWatchBuilder {

    public static final long DEFAULT_DELAY_BETWEEN_READS_IN_MILLISECONDS = 1000;
    public static final int DEFAULT_DELAY_BETWEEN_SWEEPS_IN_MILLISECONDS = 1000;
    public static final int DEFAULT_READ_BUFFER_SIZE_IN_BYTES = 4096;

    /**
     * Used to construct a {@link LogWatch} for a particular log file.
     * 
     * @param f
     *            File to watch.
     * @return Builder that is used to configure the new log watch instance
     *         before using.
     */
    public static LogWatchBuilder forFile(final File f) {
        return new LogWatchBuilder(f);
    }

    private final File fileToWatch;
    private int limitCapacityTo = Integer.MAX_VALUE;
    private long delayBetweenSweeps = LogWatchBuilder.DEFAULT_DELAY_BETWEEN_READS_IN_MILLISECONDS;
    private long delayBetweenReads = LogWatchBuilder.DEFAULT_DELAY_BETWEEN_READS_IN_MILLISECONDS;
    private boolean readingFromBeginning = true;
    private boolean closingBetweenReads = false;
    // will accept all messages
    private MessageCondition messageAcceptanceCondition = AllMessagesAcceptingCondition.INSTANCE;

    private int bufferSize = LogWatchBuilder.DEFAULT_READ_BUFFER_SIZE_IN_BYTES;

    protected LogWatchBuilder(final File fileToWatch) {
        this.fileToWatch = fileToWatch;
    }

    /**
     * Build the log watch with previously defined properties, or defaults where
     * not overriden.
     */
    public LogWatch build() {
        return this.buildWith(new SimpleTailSplitter());
    }

    /**
     * The condition that will be used for accepting messages into the log
     * watch.
     * 
     * @return The condition.
     */
    public MessageCondition getMessageAcceptanceCondition() {
        return this.messageAcceptanceCondition;
    }

    /**
     * Only the messages for which {@link MessageCondition#accept(Message)} is
     * true will be registered by the future log watch.
     * 
     * @param condition
     *            The condition.
     * @return This.
     */
    public LogWatchBuilder withMessageAcceptanceCondition(final MessageCondition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Message acceptance condition must not be null.");
        }
        this.messageAcceptanceCondition = condition;
        return this;
    }

    /**
     * Build the log watch with previously defined properties, or defaults where
     * not overriden.
     * 
     * @param splitter
     *            The splitter instance to use for the log watch instead of the
     *            default.
     * @return This.
     */
    public LogWatch buildWith(final TailSplitter splitter) {
        if (splitter == null) {
            throw new IllegalArgumentException("A splitter must be provided.");
        }
        return new DefaultLogWatch(this.fileToWatch, splitter, this.limitCapacityTo, this.messageAcceptanceCondition,
                this.delayBetweenReads, this.delayBetweenSweeps, !this.readingFromBeginning, this.closingBetweenReads, this.bufferSize);
    }

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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final LogWatchBuilder other = (LogWatchBuilder) obj;
        if (this.bufferSize != other.bufferSize) {
            return false;
        }
        if (this.closingBetweenReads != other.closingBetweenReads) {
            return false;
        }
        if (this.delayBetweenReads != other.delayBetweenReads) {
            return false;
        }
        if (this.fileToWatch == null) {
            if (other.fileToWatch != null) {
                return false;
            }
        } else if (!this.fileToWatch.equals(other.fileToWatch)) {
            return false;
        }
        if (this.limitCapacityTo != other.limitCapacityTo) {
            return false;
        }
        if (this.messageAcceptanceCondition == null) {
            if (other.messageAcceptanceCondition != null) {
                return false;
            }
        } else if (!this.messageAcceptanceCondition.equals(other.messageAcceptanceCondition)) {
            return false;
        }
        if (this.readingFromBeginning != other.readingFromBeginning) {
            return false;
        }
        return true;
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

    /**
     * Get the delay between attempts to sweep unreachable messages from memory.
     * 
     * @return In milliseconds.
     */
    public long getDelayBetweenSweeps() {
        return this.delayBetweenSweeps;
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
     * Get the file that the log watch will be watching.
     */
    public File getFileToWatch() {
        return this.fileToWatch;
    }

    /**
     * Get the buffer size for the log watch.
     * 
     * @return In bytes.
     */
    public int getReadingBufferSize() {
        return this.bufferSize;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + this.bufferSize;
        result = (prime * result) + (this.closingBetweenReads ? 1231 : 1237);
        result = (prime * result) + (int) (this.delayBetweenReads ^ (this.delayBetweenReads >>> 32));
        result = (prime * result) + ((this.fileToWatch == null) ? 0 : this.fileToWatch.hashCode());
        result = (prime * result) + this.limitCapacityTo;
        result = (prime * result)
                + ((this.messageAcceptanceCondition == null) ? 0 : this.messageAcceptanceCondition.hashCode());
        result = (prime * result) + (this.readingFromBeginning ? 1231 : 1237);
        return result;
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
     * Whether or not the file will be closed after it is read from.
     */
    public boolean isClosingBetweenReads() {
        return this.closingBetweenReads;
    }

    /**
     * Whether or not the file will be read from the beginning, or just the
     * additions made post starting the log watch.
     */
    public boolean isReadingFromBeginning() {
        return this.readingFromBeginning;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LogWatchBuilder [");
        if (this.fileToWatch != null) {
            builder.append("fileToWatch=").append(this.fileToWatch).append(", ");
        }
        builder.append("limitCapacityTo=").append(this.limitCapacityTo).append(", bufferSize=").append(this.bufferSize)
                .append(", readingFromBeginning=").append(this.readingFromBeginning).append(", delayBetweenReads=")
                .append(this.delayBetweenReads).append(", closingBetweenReads=").append(this.closingBetweenReads)
                .append(", ");
        if (this.messageAcceptanceCondition != null) {
            builder.append("messageAcceptanceCondition=").append(this.messageAcceptanceCondition);
        }
        builder.append("]");
        return builder.toString();
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

}
