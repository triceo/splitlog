package com.github.triceo.splitlog;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.input.Tailer;

public class LogWatch {
    
    private final Tailer tailer;
    private final AtomicBoolean isTerminated = new AtomicBoolean(false);
    private final Set<LogTailer> tailers = new LinkedHashSet<LogTailer>();
    private final LogWatchTailerListener listener;

    protected LogWatch(final File watchedFile, final TailSplitter splitter, final MessageClassifier<MessageType> typeClassifier, final MessageClassifier<MessageSeverity> severityClassifier) {
        // FIXME this needs to be a generic logwatch, not server log watch
        this.listener = new LogWatchTailerListener(new JBossServerLogTailSplitter());
        this.tailer = new Tailer(watchedFile, listener);
        tailer.run();
    }
    
    public boolean stopTailing(LogTailer tail) {
        tailers.remove(tail);
        return true;
    }
    
    public boolean isTailing(LogTailer tail) {
        return tailers.contains(tail);
    }
    
    public LogTailer startTailing() {
        LogTailer tail = new LogTailer();
        tailers.add(tail);
        return tail;
    }
    
    public boolean isTerminated() {
        return this.isTerminated.get();
    }
    
    public void terminate() {
        this.tailer.stop();
        for (LogTailer chunk: new ArrayList<LogTailer>(this.tailers)) {
            this.stopTailing(chunk);
        }
        this.isTerminated.set(false);
    }
}
