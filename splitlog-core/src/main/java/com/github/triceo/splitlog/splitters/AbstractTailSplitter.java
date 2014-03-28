package com.github.triceo.splitlog.splitters;

import java.util.List;

import com.github.triceo.splitlog.api.ExceptionDescriptor;
import com.github.triceo.splitlog.api.TailSplitter;
import com.github.triceo.splitlog.splitters.exceptions.DefaultExceptionDescriptor;

abstract class AbstractTailSplitter implements TailSplitter {

    @Override
    public ExceptionDescriptor determineException(final List<String> raw) {
        return DefaultExceptionDescriptor.parseStackTrace(raw);
    }

}
