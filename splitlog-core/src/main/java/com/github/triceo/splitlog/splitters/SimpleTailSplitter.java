package com.github.triceo.splitlog.splitters;

import java.util.Calendar;
import java.util.Date;

import com.github.triceo.splitlog.MessageSeverity;
import com.github.triceo.splitlog.MessageType;

public class SimpleTailSplitter extends AbstractTailSplitter {

    @Override
    protected Date determineDate(final RawMessage message) {
        return Calendar.getInstance().getTime();
    }

    @Override
    protected MessageSeverity determineSeverity(final RawMessage message) {
        return MessageSeverity.UNKNOWN;
    }

    @Override
    protected MessageType determineType(final RawMessage message) {
        return MessageType.LOG;
    }

    @Override
    protected boolean isStartingLine(final String line) {
        return true;
    }

    @Override
    protected String stripOfMetadata(final String line) {
        return line;
    }

}
