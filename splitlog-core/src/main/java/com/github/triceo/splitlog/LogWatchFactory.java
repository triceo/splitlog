package com.github.triceo.splitlog;

import java.io.File;

public class LogWatchFactory {

    public static LogWatch newLogWatch(final File logToWatch) {
        return new LogWatch(logToWatch, null, null, null);
    }

}
