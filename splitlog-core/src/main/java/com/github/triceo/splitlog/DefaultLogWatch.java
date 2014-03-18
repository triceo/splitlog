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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.triceo.splitlog.conditions.MessageCondition;
import com.github.triceo.splitlog.splitters.TailSplitter;

/**
 * Default log watch implementation which provides all the bells and whistles so
 * that the rest of the tool can work together.
 * 
 */
final class DefaultLogWatch implements LogWatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLogWatch.class);

    private final AtomicInteger numberOfActiveTailers = new AtomicInteger(0);
    private Tailer tailer;
    private final TailSplitter splitter;
    private final AtomicBoolean isTerminated = new AtomicBoolean(false);
    private final Set<AbstractLogTailer> tailers = new LinkedHashSet<AbstractLogTailer>();
    private final LogWatchTailerListener listener;
    private final File watchedFile;
    /**
     * These maps are weak; when a tailer stops being used by user code, we do
     * not want these IDs to prevent it from being GC'd. Yet, for as long as the
     * tailer is being used, we want to keep the IDs since the tailer may still
     * ask for the messages.
     */
    private final Map<LogTailer, Integer> startingMessageIds = new WeakHashMap<LogTailer, Integer>(),
            endingMessageIds = new WeakHashMap<LogTailer, Integer>();

    private final MessageStore messages;
    private final MessageCondition acceptanceCondition;
    private final long delayBetweenReads;
    private final int bufferSize;
    private final boolean reopenBetweenReads, ignoreExistingContent;

    protected DefaultLogWatch(final File watchedFile, final TailSplitter splitter, final int capacity,
            final MessageCondition acceptanceCondition, final long delayBetweenReads,
            final boolean ignoreExistingContent, final boolean reopenBetweenReads, final int bufferSize) {
        this.acceptanceCondition = acceptanceCondition;
        this.messages = new MessageStore(capacity);
        this.splitter = splitter;
        // for the tailer
        this.bufferSize = bufferSize;
        this.delayBetweenReads = delayBetweenReads;
        this.reopenBetweenReads = reopenBetweenReads;
        this.ignoreExistingContent = ignoreExistingContent;
        this.watchedFile = watchedFile;
        this.listener = new LogWatchTailerListener(this);
    }

    private MessageBuilder currentlyProcessedMessage = null;

    private int getNumberOfActiveTailers() {
        return this.numberOfActiveTailers.get();
    }

    protected synchronized void addLine(final String line) {
        final boolean isMessageBeingProcessed = this.currentlyProcessedMessage != null;
        if (this.splitter.isStartingLine(line)) {
            // new message begins
            if (isMessageBeingProcessed) { // finish old message
                this.handleCompleteMessage(this.currentlyProcessedMessage);
            }
            // prepare for new message
            this.currentlyProcessedMessage = new MessageBuilder(line);
        } else {
            // continue present message
            if (!isMessageBeingProcessed) {
                // most likely just a garbage immediately after start
                return;
            }
            this.currentlyProcessedMessage.addLine(line);
        }
        this.handleIncomingMessage(this.currentlyProcessedMessage);
    }

    private synchronized void handleIncomingMessage(final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildIntermediate(this.splitter);
        for (final AbstractLogTailer t : this.tailers) {
            t.notifyOfIncomingMessage(message);
        }
    }

    private synchronized void handleUndeliveredMessage(final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildIntermediate(this.splitter);
        for (final AbstractLogTailer t : this.tailers) {
            t.notifyOfUndeliveredMessage(message);
        }
    }

    private synchronized void handleCompleteMessage(final MessageBuilder messageBuilder) {
        final Message message = messageBuilder.buildFinal(this.splitter);
        final boolean messageAccepted = this.acceptanceCondition.accept(message);
        if (messageAccepted) {
            this.messages.add(message);
        } else {
            DefaultLogWatch.LOGGER.info("Filter rejected message '{}' from file {}.", message, this.watchedFile);
        }
        for (final AbstractLogTailer t : this.tailers) {
            if (messageAccepted) {
                t.notifyOfAcceptedMessage(message);
            } else {
                t.notifyOfRejectedMessage(message);
            }
        }
    }

    /**
     * Return all messages that have been sent to the tailer, from its start
     * until either its termination or to this moment, whichever is relevant.
     * 
     * This method is synchronized so that the modification of the underlying
     * message store in {@link #addLine(String)} and the reading of this store
     * is mutually excluded. Otherwise, there is a possibility of message ID
     * mess in the discarding case.
     * 
     * @param tail
     *            The tailer in question.
     * @return Unmodifiable map of all the received messages, with keys being
     *         IDs of those messages.
     */
    protected synchronized SortedMap<Integer, Message> getAllMessages(final LogTailer tail) {
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
        if (this.getNumberOfActiveTailers() < 1) {
            this.startTailer();
        }
        final int startingMessageId = this.messages.getNextMessageId();
        final AbstractLogTailer tail = new NonStoringLogTailer(this);
        this.tailers.add(tail);
        this.startingMessageIds.put(tail, startingMessageId);
        this.numberOfActiveTailers.incrementAndGet();
        return tail;
    }

    @Override
    public synchronized boolean terminateTailing() {
        if (this.isTerminated()) {
            return false;
        }
        this.isTerminated.set(true);
        if (this.currentlyProcessedMessage != null) {
            this.handleUndeliveredMessage(this.currentlyProcessedMessage);
        }
        for (final AbstractLogTailer chunk : new ArrayList<AbstractLogTailer>(this.tailers)) {
            this.terminateTailing(chunk);
        }
        return true;
    }

    private final ExecutorService e = Executors.newSingleThreadExecutor();

    private final boolean startTailer() {
        this.tailer = new Tailer(this.watchedFile, this.listener, this.delayBetweenReads, this.ignoreExistingContent,
                this.reopenBetweenReads, this.bufferSize);
        this.e.execute(this.tailer);
        DefaultLogWatch.LOGGER.debug("Started log watch for file '{}'", this.watchedFile);
        return true;
    }

    private final boolean terminateTailer() {
        this.tailer.stop();
        this.tailer = null;
        DefaultLogWatch.LOGGER.debug(
                "Terminated log watch for file '{}' as the last known LogTailer has just been terminated.",
                this.watchedFile);
        return true;
    }

    @Override
    public synchronized boolean terminateTailing(final LogTailer tail) {
        if (this.tailers.remove(tail)) {
            final int endingMessageId = this.messages.getLatestMessageId();
            this.endingMessageIds.put(tail, endingMessageId);
            this.numberOfActiveTailers.decrementAndGet();
            if (this.getNumberOfActiveTailers() == 0) {
                this.terminateTailer();
            }
            return true;
        } else {
            return false;
        }
    }
}
