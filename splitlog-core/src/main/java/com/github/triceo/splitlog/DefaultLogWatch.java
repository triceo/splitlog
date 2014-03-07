package com.github.triceo.splitlog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.input.Tailer;

import com.github.triceo.splitlog.splitters.TailSplitter;

/**
 * Default log watch implementation which provides all the bells and whistles so
 * that the rest of the tool can work together.
 * 
 */
final class DefaultLogWatch implements LogWatch {

    private final Tailer tailer;
    private final TailSplitter splitter;
    private final AtomicBoolean isTerminated = new AtomicBoolean(false);
    private final Set<AbstractLogTailer> tailers = new LinkedHashSet<AbstractLogTailer>();
    private final LogWatchTailerListener listener;
    /**
     * These maps are weak; when a tailer stops being used by user code, we do
     * not want these IDs to prevent it from being GC'd. Yet, for as long as the
     * tailer is being used, we want to keep the IDs since the tailer may still
     * ask for the messages.
     */
    private final Map<LogTailer, Integer> startingMessageIds = new WeakHashMap<LogTailer, Integer>(),
            endingMessageIds = new WeakHashMap<LogTailer, Integer>();

    private final MessageStore messages;

    protected DefaultLogWatch(final File watchedFile, final TailSplitter splitter, final int capacity,
            final long delayBetweenReads, final boolean ignoreExistingContent, final boolean reopenBetweenReads,
            final int bufferSize) {
        this.messages = new MessageStore(capacity);
        this.listener = new LogWatchTailerListener(this);
        this.splitter = splitter;
        this.tailer = Tailer.create(watchedFile, this.listener, delayBetweenReads, ignoreExistingContent,
                reopenBetweenReads, bufferSize);
    }

    protected synchronized void addLine(final String line) {
        final Message message = this.splitter.addLine(line);
        if (message != null) {
            this.messages.add(message);
        }
        for (final AbstractLogTailer t : this.tailers) {
            t.notifyOfLine(line);
            if (message != null) {
                t.notifyOfMessage(message);
            }
        }
    }

    /**
     * Return all messages that have been sent to the tailer, from its start
     * until either its termination or to this moment, whichever is relevant.
     * 
     * @param tail
     *            The tailer in question.
     * @return Unmodifiable map of all the received messages, with keys being
     *         IDs of those messages.
     */
    protected SortedMap<Integer, Message> getAllMessages(final LogTailer tail) {
        final int start = this.getStartingMessageId(tail);
        // get the expected ending message ID
        final int end = this.getEndingMessageId(tail);
        if (start > end) {
            /*
             * in case some messages have been discarded, the actual start may
             * get ahead of the expected end. this would have caused an
             * exception within the message store, and so we handle it here and
             * return an empty list. this is exactly correct, as if the end is
             * before the first message in the store, there really is nothing to
             * return.
             */
            return Collections.unmodifiableSortedMap(new TreeMap<Integer, Message>());
        } else {
            final SortedMap<Integer, Message> messages = new TreeMap<Integer, Message>();
            int id = start;
            for (final Message msg : this.messages.getFromRange(start, end + 1)) {
                messages.put(id, msg);
                id++;
            }
            return Collections.unmodifiableSortedMap(messages);
        }
    }

    /**
     * Get index of the last plus one message that the tailer has access to.
     * 
     * @param tail
     *            Tailer in question.
     */
    protected int getEndingMessageId(final LogTailer tail) {
        return this.endingMessageIds.containsKey(tail) ? this.endingMessageIds.get(tail) : this.messages
                .getLatestMessageId();
    }

    /**
     * If messages have been discarded, the original starting message ID will no
     * longer be valid. therefore, we check for the actual starting ID.
     * 
     * @param tail
     *            Tailer in question.
     */
    protected int getStartingMessageId(final LogTailer tail) {
        return Math.max(this.messages.getFirstMessageId(), this.startingMessageIds.get(tail));
    }

    @Override
    public boolean isTerminated() {
        return this.isTerminated.get();
    }

    @Override
    public boolean isTerminated(final LogTailer tail) {
        return !this.tailers.contains(tail);
    }

    @Override
    public synchronized LogTailer startTailing() {
        if (this.isTerminated()) {
            throw new IllegalStateException("Cannot start tailing on an already terminated LogWatch.");
        }
        final int startingMessageId = this.messages.getNextMessageId();
        final AbstractLogTailer tail = new NonStoringLogTailer(this);
        this.tailers.add(tail);
        this.startingMessageIds.put(tail, startingMessageId);
        return tail;
    }

    @Override
    public synchronized boolean terminateTailing() {
        if (this.isTerminated()) {
            return false;
        }
        this.tailer.stop();
        final Message message = this.splitter.forceProcessing();
        for (final AbstractLogTailer chunk : new ArrayList<AbstractLogTailer>(this.tailers)) {
            if (message != null) {
                chunk.notifyOfMessage(message);
            }
            this.terminateTailing(chunk);
        }
        this.isTerminated.set(true);
        return true;
    }

    @Override
    public synchronized boolean terminateTailing(final LogTailer tail) {
        if (this.tailers.remove(tail)) {
            final int endingMessageId = this.messages.getLatestMessageId();
            this.endingMessageIds.put(tail, endingMessageId);
            return true;
        } else {
            return false;
        }
    }
}
