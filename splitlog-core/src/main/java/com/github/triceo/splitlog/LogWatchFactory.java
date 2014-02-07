package com.github.triceo.splitlog;

import java.io.File;

import com.github.triceo.splitlog.splitters.SimpleTailSplitter;
import com.github.triceo.splitlog.splitters.TailSplitter;

public class LogWatchFactory {

    public static LogWatch newLogWatch(final File logToWatch) {
        return new LogWatch(logToWatch, new SimpleTailSplitter());
    }

    public static LogWatch newLogWatch(final File logToWatch, final TailSplitter splitter) {
        return new LogWatch(logToWatch, splitter);
    }

}
