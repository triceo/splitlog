package com.github.triceo.splitlog;

import java.io.File;

public class LogWatchFactory {

    public static LogWatch newLogWatch(final File logToWatch) {
        return new LogWatch(logToWatch, new DefaultTailSplitter(), new MessageClassifier<MessageType>() {

            public MessageType classify(final RawMessage m) {
                return MessageType.LOG;
            }

        }, new MessageClassifier<MessageSeverity>() {

            public MessageSeverity classify(final RawMessage m) {
                return MessageSeverity.UNKNOWN;
            }

        });
    }

}
