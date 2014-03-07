package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;

/**
 * Data storage for a particular {@link LogWatch}. Currently it is only a
 * synchronization layer on top of a list, but in the future it may abstract
 * away features like off-memory storage etc.
 * 
 * This class is thread-safe.
 */
class MessageStore {

    private static final int INITIAL_MESSAGE_ID = 0;

    private final List<Message> store = new LinkedList<Message>();
    private int lastMessageId = MessageStore.INITIAL_MESSAGE_ID - 1;
    private int numOfDiscardedMessages = MessageStore.INITIAL_MESSAGE_ID;
    private final int messageLimit;

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
            // LinkedList.size() is int; just in case
            throw new IllegalArgumentException(
                    "The message storage cannot handle a capacity larger than Integer.MAX_VALUE.");
        } else {
            this.messageLimit = size;
        }
    }

    /**
     * Add message to the storage.
     * 
     * @param msg
     *            Message in question.
     * @return ID of the message.
     */
    public synchronized int add(final Message msg) {
        this.store.add(msg);
        if (this.store.size() > this.messageLimit) {
            // discard first message if we're over limit
            this.store.remove(0);
            this.numOfDiscardedMessages++;
        }
        return this.lastMessageId++;
    }

    /**
     * The latest ID that has been given out to a message.
     * 
     * @return -1 if no messages yet.
     */
    public synchronized int getLatestMessageId() {
        return this.lastMessageId;
    }

    /**
     * ID of the message that comes first in this store. This number will
     * increase as messages are discarded due to storage capacity.
     * 
     * @return -1 if no messages yet. 0 if no messages have been discarded. Add
     *         one for every discarded message.
     */
    public synchronized int getFirstMessageId() {
        if (this.getNextMessageId() == MessageStore.INITIAL_MESSAGE_ID) {
            return -1;
        } else {
            return this.numOfDiscardedMessages + MessageStore.INITIAL_MESSAGE_ID;
        }
    }

    /**
     * The ID that will be given out to the message that goes through the very
     * next {@link #add(Message)} call.
     * 
     * @return 0 if no messages yet.
     */
    public int getNextMessageId() {
        return this.getLatestMessageId() + 1;
    }

    /**
     * This is synchronized in order to mutually exclude changing the store (by
     * the also synchronized {@link #add(Message)} method) with getting the size
     * of the store in this method. Otherwise
     * {@link ConcurrentModificationException} may ensue.
     * 
     * @param startId
     *            Left-most position in the message store. (Not the message ID.)
     * @param endId
     *            Right-most position plus one in the message-store. (Not the
     *            message ID.)
     * @return A copy of the sub-list provided by the message store.
     */
    private synchronized List<Message> actuallyGetFromRange(final int startId, final int endId) {
        return new LinkedList<Message>(this.store.subList(startId, endId));
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
    public List<Message> getFromRange(final int startId, final int endId) {
        // cache this here, so all parts of the method operate on the same data
        final int firstMessageId = this.getFirstMessageId();
        // input validation
        if ((startId < 0) || (endId < 0)) {
            throw new IllegalArgumentException("Message ID cannot be negative.");
        } else if ((firstMessageId >= 0) && (startId < firstMessageId)) {
            throw new IllegalArgumentException("Message with ID " + startId
                    + " had already been discarded. First available message has ID " + firstMessageId + ".");
        } else if (endId <= startId) {
            throw new IllegalArgumentException("Ending message ID must me larger than starting message ID.");
        } else if (endId > this.getNextMessageId()) {
            throw new IllegalArgumentException("Range end cannot be greater than the next message ID.");
        }
        // and properly synchronized range retrieval
        return Collections
                .unmodifiableList(this.actuallyGetFromRange(startId - firstMessageId, endId - firstMessageId));
    }

    /**
     * Return all messages with IDs larger or equal to the given ID.
     * 
     * @param startId
     *            Least id, inclusive.
     * @return Unmodifiable list containing those messages.
     */
    public List<Message> getFrom(final int startId) {
        return this.getFromRange(startId, this.getNextMessageId());
    }

}
