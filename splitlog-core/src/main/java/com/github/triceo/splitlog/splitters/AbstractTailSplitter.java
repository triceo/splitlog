package com.github.triceo.splitlog.splitters;

import com.github.triceo.splitlog.MessageBuilder;
import com.github.triceo.splitlog.exceptions.ExceptionDescriptor;

abstract class AbstractTailSplitter implements TailSplitter {

    @Override
    public ExceptionDescriptor determineException(final MessageBuilder message) {
        return ExceptionDescriptor.parseStackTrace(message.getLines());
    }

}
