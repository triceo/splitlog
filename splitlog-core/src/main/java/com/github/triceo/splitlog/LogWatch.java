package com.github.triceo.splitlog;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.input.Tailer;

public class LogWatch {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Tailer tailer;
    private final AtomicBoolean isTerminated = new AtomicBoolean(false);
    private final Set<AbstractLogTailer> tailers = new LinkedHashSet<AbstractLogTailer>();
    private final LogWatchTailerListener listener;
    // these hashmaps are weak; when a tailer is terminated and removed from
    // logwatch, we don't want to keep it anymore
    private final Map<AbstractLogTailer, Integer> startingMessageIds = new WeakHashMap<AbstractLogTailer, Integer>(),
            endingMessageIds = new WeakHashMap<AbstractLogTailer, Integer>();

    private final List<Message> messageQueue = new CopyOnWriteArrayList<Message>();

    protected LogWatch(final File watchedFile, final TailSplitter splitter) {
        this.listener = new LogWatchTailerListener(this, splitter);
        this.tailer = new Tailer(watchedFile, this.listener);
        this.executor.execute(this.tailer);
    }

    protected void addMessage(final Message message) {
        this.messageQueue.add(message);
        for (final AbstractLogTailer t : this.tailers) {
            t.notifyOfMessage(message);
        }
    }

    protected List<Message> getAllMessages(final AbstractLogTailer tail) {
        return new ArrayList<Message>(this.messageQueue.subList(this.startingMessageIds.get(tail),
                this.getEndingId(tail)));
    }

    /**
     * Get index of the last plus one message that the tailer has access to.
     * 
     * @param tail
     *            Tailer in question.
     * @return
     */
    private int getEndingId(final AbstractLogTailer tail) {
        return this.endingMessageIds.containsKey(tail) ? this.endingMessageIds.get(tail) : this.messageQueue.size();
    }

    protected Message getMessage(final AbstractLogTailer tail, final int index) {
        if (!this.startingMessageIds.containsKey(tail)) {
            throw new IllegalArgumentException("Unknown tailer: " + tail);
        } else if (index < 0) {
            throw new IllegalArgumentException("Message index must be >= 0.");
        }
        final int startingId = this.startingMessageIds.get(tail);
        final int endingId = this.getEndingId(tail);
        final int maxIndex = endingId - startingId;
        if (index > maxIndex) {
            throw new IllegalArgumentException("No messages past index " + maxIndex + ".");
        }
        return this.messageQueue.get(index + startingId);
    }

    public boolean isTailing(final AbstractLogTailer tail) {
        return this.tailers.contains(tail);
    }

    public boolean isTerminated() {
        return this.isTerminated.get();
    }

    public AbstractLogTailer startTailing() {
        final int startingMessageId = this.messageQueue.size();
        final AbstractLogTailer tail = new NonStoringLogTailer(this);
        this.tailers.add(tail);
        this.startingMessageIds.put(tail, startingMessageId);
        return tail;
    }

    public boolean terminateTailing() {
        final boolean isTailing = !this.isTerminated();
        if (!isTailing) {
            return false;
        }
        this.tailer.stop();
        for (final AbstractLogTailer chunk : new ArrayList<AbstractLogTailer>(this.tailers)) {
            this.terminateTailing(chunk);
        }
        this.isTerminated.set(false);
        this.executor.shutdownNow();
        return true;
    }

    public boolean terminateTailing(final AbstractLogTailer tail) {
        this.tailers.remove(tail);
        final int endingMessageId = this.messageQueue.size();
        this.endingMessageIds.put(tail, endingMessageId);
        return true;
    }
}
