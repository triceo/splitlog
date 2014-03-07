package com.github.triceo.splitlog.splitters;

import com.github.triceo.splitlog.MessageBuilder;
import com.github.triceo.splitlog.exceptions.ExceptionDescriptor;

abstract class AbstractTailSplitter implements TailSplitter {

    /**
     * Read the message and try to identify an exception stack trace within.
     * 
     * @param message
     *            Message in question.
     * @return Exception data if found, null otherwise.
     */
    @Override
    public ExceptionDescriptor determineException(final MessageBuilder message) {
        return ExceptionDescriptor.parseStackTrace(message.getLines());
    }

}
