package com.github.triceo.splitlog.splitters;

import com.github.triceo.splitlog.Message;

public interface TailSplitter {

    /**
     * Read one line from the log and convert it into a message, if possible.
     * 
     * Message will only be returned after all of its lines have been through
     * this method. Therefore, it is likely that an actual Message instance
     * would only be returned when the first line of a new message is submitted.
     * 
     * @param line
     *            Line from the tailed file.
     * @return Message element if considered to be a complete message. Null in
     *         case that the line does not end the message.
     */
    Message addLine(String line);

    /**
     * Will convert existing lines into a message, regardless whether the
     * message is complete or not.
     * 
     * @return Message element, even if message isn't completely read; or null
     *         if no message.
     */
    Message forceProcessing();

}
