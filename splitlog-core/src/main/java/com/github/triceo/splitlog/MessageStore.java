package com.github.triceo.splitlog;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data storage for a particular {@link LogWatch}. Currently it is only a
 * synchronization layer on top of a list, but in the future it may abstract
 * away features like off-memory storage etc.
 * 
 * This class is thread-safe.
 */
class MessageStore {

    private final List<Message> store = new LinkedList<Message>();
    private final AtomicInteger lastMessageId = new AtomicInteger(-1);

    /**
     * Add message to the storage.
     * 
     * @param msg
     *            Message in question.
     * @return ID of the message.
     */
    public synchronized int add(final Message msg) {
        this.store.add(msg);
        return this.lastMessageId.incrementAndGet();
    }

    /**
     * The latest ID that has been given out to a message.
     * 
     * @return -1 if no messages yet.
     */
    public int getLatestMessageId() {
        return this.lastMessageId.get();
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
     *            Least id, inclusive.
     * @param endId
     *            Greatest id, exclusive.
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
     * @return Unmodifiable list containing those images.
     */
    public List<Message> getFromRange(final int startId, final int endId) {
        if ((startId < 0) || (endId < 0)) {
            throw new IllegalArgumentException("Message ID cannot be negative.");
        } else if (endId <= startId) {
            throw new IllegalArgumentException("Ending message ID must me larger than starting message ID.");
        } else if (endId > this.getNextMessageId()) {
            throw new IllegalArgumentException("Range end cannot be greater than the next message ID.");
        }
        return Collections.unmodifiableList(this.actuallyGetFromRange(startId, endId));
    }

}
