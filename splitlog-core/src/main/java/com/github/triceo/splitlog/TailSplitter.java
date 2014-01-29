package com.github.triceo.splitlog;

public interface TailSplitter {

    /**
     * Read one line from the log and convert it into a message, if possible.
     * 
     * @param line
     *            Line from the tailed file.
     * @return Message element if considered to be a full message. Null in case
     *         that this line does not end the message.
     */
    RawMessage addLine(String line);

    /**
     * Will convert existing lines into a message, regardless whether the
     * message is complete or not.
     * 
     * @return
     */
    RawMessage forceProcessing();

}
