package com.github.triceo.splitlog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /*
     * these hashmaps are weak; when a tailer is terminated and removed from
     * logwatch, we don't want to keep it anymore
     */
    private final Map<LogTailer, Integer> startingMessageIds = new WeakHashMap<LogTailer, Integer>(),
            endingMessageIds = new WeakHashMap<LogTailer, Integer>();

    private final List<Message> messageQueue = Collections.synchronizedList(new LinkedList<Message>());

    protected DefaultLogWatch(final File watchedFile, final TailSplitter splitter, final long delayBetweenReads,
            final boolean ignoreExistingContent, final boolean reopenBetweenReads, final int bufferSize) {
        this.listener = new LogWatchTailerListener(this);
        this.splitter = splitter;
        this.tailer = Tailer.create(watchedFile, this.listener, delayBetweenReads, ignoreExistingContent,
                reopenBetweenReads, bufferSize);
    }

    protected void addLine(final String line) {
        final Message message = this.splitter.addLine(line);
        if (message != null) {
            // synchronized writing
            this.messageQueue.add(message);
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
     * @return Unmodifiable list of all the received messages.
     */
    protected List<Message> getAllMessages(final LogTailer tail) {
        final int start = this.startingMessageIds.get(tail);
        final int end = this.getEndingId(tail);
        if (start == end) {
            List<Message> empty = Collections.emptyList();
            return Collections.unmodifiableList(empty);
        }
        List<Message> sub;
        // making sublist of messageQueue needs to be synchronized
        // http://docs.oracle.com/javase/6/docs/api/java/util/Collections.html#synchronizedList%28java.util.List%29
        synchronized (this.messageQueue) {
            // subList() only returns a view of the original collection, whose iteration needs to be explicitely
            // synchronized even if the collection itself is synchronized. Thus, we need a copy of messageQueue, that
            // can be safely iterated over (independently on writing to messageQueue).
            // On the other hand, no need to copy individual Messages as they are immutable.
            sub = new ArrayList<Message>(this.messageQueue.subList(start, end));
        }
        return Collections.unmodifiableList(sub);
    }

    /**
     * Get index of the last plus one message that the tailer has access to.
     * 
     * @param tail
     *            Tailer in question.
     */
    private int getEndingId(final LogTailer tail) {
        return this.endingMessageIds.containsKey(tail) ? this.endingMessageIds.get(tail) : this.messageQueue.size();
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
    public LogTailer startTailing() {
        final int startingMessageId = this.messageQueue.size();
        final AbstractLogTailer tail = new NonStoringLogTailer(this);
        this.tailers.add(tail);
        this.startingMessageIds.put(tail, startingMessageId);
        return tail;
    }

    @Override
    public boolean terminateTailing() {
        final boolean isTailing = !this.isTerminated();
        if (!isTailing) {
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
    public boolean terminateTailing(final LogTailer tail) {
        if (this.tailers.remove(tail)) {
            final int endingMessageId = this.messageQueue.size();
            this.endingMessageIds.put(tail, endingMessageId);
            return true;
        } else {
            return false;
        }
    }
}
