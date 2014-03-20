package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data storage for a particular {@link LogWatch}.
 * 
 * This class is thread-safe.
 */
final class MessageStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageStore.class);
    private static final int INITIAL_MESSAGE_ID = 0;

    private final SortedMap<Integer, Message> store = new TreeMap<Integer, Message>();
    private final int messageLimit;
    private int nextMessageId = MessageStore.INITIAL_MESSAGE_ID;

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
        } else if (size > Integer.MAX_VALUE) {
            // the map's size() is int
            throw new IllegalArgumentException(
                    "The message storage cannot handle a capacity larger than Integer.MAX_VALUE.");
        } else {
            this.messageLimit = size;
        }
    }

    /**
     * Remove messages from the queue that are older than the given ID. If the
     * ID is larger than {@link #getLatestMessageId()}, all messages will be
     * discarded while marking no future messages for discarding.
     * 
     * @param firstKeyNotToDiscard
     *            ID of the first message to be kept.
     * @return Number of messages actually discarded.
     */
    public synchronized int discardBefore(final int firstKeyNotToDiscard) {
        final int firstMessageId = this.getFirstMessageId();
        if (this.getNextMessageId() == MessageStore.INITIAL_MESSAGE_ID) {
            MessageStore.LOGGER.info("Not discarding any messages, as there haven't been any messages yet.");
            return 0;
        } else if ((firstKeyNotToDiscard < MessageStore.INITIAL_MESSAGE_ID) || (firstKeyNotToDiscard <= firstMessageId)) {
            MessageStore.LOGGER.info("Not discarding any messages, as there are no messages with ID lower than {}.",
                    firstKeyNotToDiscard);
            return 0;
        } else if (firstKeyNotToDiscard > this.getLatestMessageId()) {
            MessageStore.LOGGER.info("Discarding all messages, as all have IDs lower than {}.", firstKeyNotToDiscard);
            final int size = this.store.size();
            this.store.clear();
            return size;
        }
        // and now actually discard
        MessageStore.LOGGER.info("Discarding messages in range <{},{}).", firstMessageId, firstKeyNotToDiscard);
        final SortedMap<Integer, Message> toDiscard = this.store.headMap(firstKeyNotToDiscard);
        final int size = toDiscard.size();
        toDiscard.clear();
        return size;
    }

    /**
     * Add message to the storage.
     * 
     * Every call will change values returned by {@link #getNextMessageId()} and
     * {@link #getLatestMessageId()}. Any call may change value returned by
     * {@link #getFirstMessageId()}, which will happen if a message is discarded
     * due to hitting the message store capacity.
     * 
     * @param msg
     *            Message in question.
     * @return ID of the message.
     */
    public synchronized int add(final Message msg) {
        final int nextKey = this.getNextMessageId();
        this.store.put(nextKey, msg);
        this.nextMessageId++;
        if (this.store.size() > this.messageLimit) {
            // discard first message if we're over limit
            this.store.remove(this.store.firstKey());
        }
        return nextKey;
    }

    /**
     * The latest ID that has been given out to a message.
     * 
     * @return -1 if no messages yet.
     */
    public synchronized int getLatestMessageId() {
        return (this.store.isEmpty()) ? this.nextMessageId - 1 : this.store.lastKey();
    }

    /**
     * ID of the message that comes first in this store.
     * 
     * @return -1 if no messages yet. 0 if no messages have been discarded. Add
     *         one for every discarded message.
     */
    public synchronized int getFirstMessageId() {
        if (this.store.isEmpty()) {
            return MessageStore.INITIAL_MESSAGE_ID - 1;
        } else {
            return this.store.firstKey();
        }
    }

    /**
     * The ID that will be given out to the message that goes through the very
     * next {@link #add(Message)} call.
     * 
     * @return 0 if no messages have been inserted yet.
     */
    public synchronized int getNextMessageId() {
        return this.nextMessageId;
    }

    /**
     * Return all messages with IDs in the given range.
     * 
     * @param startId
     *            Least id, inclusive.
     * @param endId
     *            Greatest id, exclusive.
     * @return Unmodifiable list containing those messages.
     */
    public synchronized List<Message> getFromRange(final int startId, final int endId) {
        // cache this here, so all parts of the method operate on the same data
        final int firstMessageId = this.getFirstMessageId();
        // input validation
        if ((startId < MessageStore.INITIAL_MESSAGE_ID) || (endId < MessageStore.INITIAL_MESSAGE_ID)) {
            throw new IllegalArgumentException("Message ID cannot be negative.");
        } else if ((firstMessageId >= MessageStore.INITIAL_MESSAGE_ID) && (startId < firstMessageId)) {
            throw new IllegalArgumentException("Message with ID " + startId
                    + " had already been discarded. First available message has ID " + firstMessageId + ".");
        } else if (endId <= startId) {
            throw new IllegalArgumentException("Ending message ID must be larger than starting message ID.");
        } else if (endId > this.getNextMessageId()) {
            throw new IllegalArgumentException("Range end cannot be greater than the next message ID.");
        }
        // and properly synchronized range retrieval
        return Collections.unmodifiableList(new LinkedList<Message>(this.store.subMap(startId, endId).values()));
    }

    /**
     * Return all messages with IDs larger or equal to the given ID.
     * 
     * @param startId
     *            Least id, inclusive.
     * @return Unmodifiable list containing those messages.
     */
    public synchronized List<Message> getFrom(final int startId) {
        return this.getFromRange(startId, this.getNextMessageId());
    }

    /**
     * Return all messages currently present.
     * 
     * @return Unmodifiable list containing those messages.
     */
    public synchronized List<Message> getAll() {
        final int firstMessageId = this.getFirstMessageId();
        if (firstMessageId < MessageStore.INITIAL_MESSAGE_ID) {
            return Collections.unmodifiableList(Collections.<Message> emptyList());
        }
        return this.getFrom(firstMessageId);
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

}
