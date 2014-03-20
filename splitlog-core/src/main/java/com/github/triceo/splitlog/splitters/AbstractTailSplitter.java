package com.github.triceo.splitlog.splitters;

import java.util.List;

import com.github.triceo.splitlog.exceptions.ExceptionDescriptor;

abstract class AbstractTailSplitter implements TailSplitter {

    @Override
    public ExceptionDescriptor determineException(final List<String> raw) {
        return ExceptionDescriptor.parseStackTrace(raw);
    }

}
