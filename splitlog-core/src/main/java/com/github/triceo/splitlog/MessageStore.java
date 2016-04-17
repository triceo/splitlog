package com.github.triceo.splitlog;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.logging.SplitlogLoggerFactory;

/**
 * Data storage for a particular {@link LogWatch}.
 *
 * This class is thread-safe.
 */
final class MessageStore {

    public static final int INITIAL_MESSAGE_POSITION = 0;
    private static final Logger LOGGER = SplitlogLoggerFactory.getLogger(MessageStore.class);

    private final int messageLimit;
    private final AtomicInteger nextMessagePosition = new AtomicInteger(MessageStore.INITIAL_MESSAGE_POSITION);
    private final Int2ObjectSortedMap<Message> store = new Int2ObjectAVLTreeMap<>();

    /**
     * Create a message store with a maximum capacity of
     * {@link Integer#MAX_VALUE} messages. Will not actually allocate all that
     * space, but instead will keep growing as necessary.
     */
    public MessageStore() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Create a message store with a given message capacity. Will not actually
     * allocate all that space, but instead will keep growing as necessary.
     */
    public MessageStore(final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("The message storage cannot have 0 or less capacity.");
        } else {
            this.messageLimit = size;
        }
    }

    /**
     * Add message to the storage.
     *
     * Every call will change values returned by {@link #getNextPosition()} and
     * {@link #getLatestPosition()}. Any call may change value returned by
     * {@link #getFirstPosition()}, which will happen if a message is discarded
     * due to hitting the message store capacity.
     *
     * @param msg
     *            Message in question.
     * @return Position of the message.
     */
    public synchronized int add(final Message msg) {
        final int nextKey = this.getNextPosition();
        this.store.put(nextKey, msg);
        MessageStore.LOGGER.info("Message #{} stored on position #{}", msg.getUniqueId(), nextKey);
        this.nextMessagePosition.incrementAndGet();
        if (this.store.size() > this.messageLimit) {
            // discard first message if we're over limit
            this.store.remove(this.store.firstKey());
        }
        return nextKey;
    }

    /**
     * The maximum number of messages that will be held by this store at a time.
     * When a message is added that pushes the store over the limit, first
     * inserted message will be removed.
     *
     * @return Maximum possible amount of messages this store can hold before it
     *         starts discarding messages.
     */
    public int capacity() {
        return this.messageLimit;
    }

    /**
     * Remove messages from the queue that come before the given position. If
     * the ID is larger than {@link #getLatestPosition()}, all messages will be
     * discarded while marking no future messages for discarding.
     *
     * @param firstPositionNotToDiscard
     *            Messages be kept from this position onward, inclusive.
     * @return Number of messages actually discarded.
     */
    public synchronized int discardBefore(final int firstPositionNotToDiscard) {
        final int firstMessagePosition = this.getFirstPosition();
        if (this.getNextPosition() == MessageStore.INITIAL_MESSAGE_POSITION) {
            MessageStore.LOGGER.info("Not discarding any messages, as there haven't been any messages yet.");
            return 0;
        } else if ((firstPositionNotToDiscard < MessageStore.INITIAL_MESSAGE_POSITION)
                || (firstPositionNotToDiscard <= firstMessagePosition)) {
            MessageStore.LOGGER.info(
                    "Not discarding any messages, as there are no messages with position lower than {}.",
                    firstPositionNotToDiscard);
            return 0;
        } else if (firstPositionNotToDiscard > this.getLatestPosition()) {
            MessageStore.LOGGER.info("Discarding all messages, as all have lower positions than {}.",
                    firstPositionNotToDiscard);
            final int size = this.store.size();
            this.store.clear();
            return size;
        }
        // and now actually discard
        MessageStore.LOGGER.info("Discarding messages in positions <{},{}).", firstMessagePosition,
                firstPositionNotToDiscard);
        final SortedMap<Integer, Message> toDiscard = this.store.headMap(firstPositionNotToDiscard);
        final int size = toDiscard.size();
        toDiscard.clear();
        return size;
    }

    /**
     * Return all messages currently present.
     *
     * @return Unmodifiable list containing those messages.
     */
    public Collection<Message> getAll() {
        final int firstMessagePosition = this.getFirstPosition();
        if (firstMessagePosition < MessageStore.INITIAL_MESSAGE_POSITION) {
            return Collections.unmodifiableList(Collections.emptyList());
        }
        return this.getFrom(firstMessagePosition);
    }

    /**
     * The first position that is occupied by a message.
     *
     * @return -1 if no messages yet. 0 if no messages have been discarded. Add
     *         one for every discarded message.
     */
    public synchronized int getFirstPosition() {
        if (this.store.isEmpty()) {
            return MessageStore.INITIAL_MESSAGE_POSITION - 1;
        } else {
            return this.store.firstIntKey();
        }
    }

    /**
     * Return all messages on positions higher or equal to the given.
     *
     * @param startPosition
     *            Least position, inclusive.
     * @return Unmodifiable list containing those messages.
     */
    public List<Message> getFrom(final int startPosition) {
        return this.getFromRange(startPosition, this.getNextPosition());
    }

    /**
     * Return all messages on positions in the given range.
     *
     * @param startPosition
     *            Least position, inclusive.
     * @param endPosition
     *            Greatest position, exclusive.
     * @return Unmodifiable list containing those messages.
     */
    public synchronized List<Message> getFromRange(final int startPosition, final int endPosition) {
        // cache this here, so all parts of the method operate on the same data
        final int firstMessageId = this.getFirstPosition();
        // input validation
        if ((startPosition < MessageStore.INITIAL_MESSAGE_POSITION)
                || (endPosition < MessageStore.INITIAL_MESSAGE_POSITION)) {
            throw new IllegalArgumentException("Message position cannot be negative.");
        } else if ((firstMessageId >= MessageStore.INITIAL_MESSAGE_POSITION) && (startPosition < firstMessageId)) {
            throw new IllegalArgumentException("Message at position " + startPosition
                    + " had already been discarded. First available message position is " + firstMessageId + ".");
        } else if (endPosition <= startPosition) {
            throw new IllegalArgumentException("Ending position must be larger than starting message position.");
        } else if (endPosition > this.getNextPosition()) {
            throw new IllegalArgumentException("Range end cannot be greater than the next message position.");
        }
        // and properly synchronized range retrieval
        return Collections.unmodifiableList(new LinkedList<>(this.store.subMap(startPosition, endPosition)
                .values()));
    }

    /**
     * The latest position that has already been filled with a message.
     *
     * @return -1 if no messages yet.
     */
    public synchronized int getLatestPosition() {
        return (this.store.isEmpty()) ? this.nextMessagePosition.get() - 1 : this.store.lastIntKey();
    }

    /**
     * The position that will be occupied by the message that goes through the
     * very next {@link #add(Message)} call.
     *
     * @return 0 if no messages have been inserted yet.
     */
    public int getNextPosition() {
        return this.nextMessagePosition.get();
    }

    /**
     * Whether or not this message store currently holds any messages.
     *
     * @return True if so, false if not. Will be false even if there were some
     *         messages before that got discarded and now there are none.
     */
    public synchronized boolean isEmpty() {
        return this.store.isEmpty();
    }

    /**
     * How many messages are currently stored here.
     *
     * It is the number of messages that went through {@link #add(Message)} and
     * that have not been discarded since, either through
     * {@link #discardBefore(int)} or automatically due to capacity.
     *
     * @return
     */
    public synchronized int size() {
        return this.store.size();
    }

}
