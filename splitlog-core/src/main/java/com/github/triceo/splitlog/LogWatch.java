package com.github.triceo.splitlog;

/**
 * The primary point of interaction with this tool. Allows users to start
 * listening to changes in log files. Use {@link LogWatchBuilder} to get an
 * instance.
 */
public interface LogWatch {

    /**
     * Whether or not {@link #terminateTailing()} has been called.
     * 
     * @return True if it has.
     */
    boolean isTerminated();

    /**
     * Whether or not {@link #terminateTailing(LogTailer)} has been
     * called for this tailer.
     * 
     * @param tail
     *            Tailer in question.
     * @return True if it has.
     */
    boolean isTerminated(final LogTailer tail);

    /**
     * Begin watching for new messages from this point in time.
     * 
     * @return API for watching for messages.
     */
    LogTailer startTailing();

    /**
     * Stop tailing for all tailers and free resources.
     * 
     * @return True if terminated as a result, false if already terminated.
     */
    boolean terminateTailing();

    /**
     * Terminate a particular tailer.
     * 
     * @param tail
     *            This tailer will receive no more messages.
     * @return True if terminated as a result, false if already terminated.
     */
    boolean terminateTailing(final LogTailer tail);
}
