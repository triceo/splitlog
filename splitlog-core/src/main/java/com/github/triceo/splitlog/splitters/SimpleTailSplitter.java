package com.github.triceo.splitlog.splitters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.github.triceo.splitlog.MessageSeverity;
import com.github.triceo.splitlog.MessageType;

final public class SimpleTailSplitter extends AbstractTailSplitter {

    @Override
    public Date determineDate(final List<String> raw) {
        return Calendar.getInstance().getTime();
    }

    @Override
    public MessageSeverity determineSeverity(final List<String> raw) {
        return MessageSeverity.UNKNOWN;
    }

    @Override
    public MessageType determineType(final List<String> raw) {
        return MessageType.LOG;
    }

    @Override
    public boolean isStartingLine(final String line) {
        return true;
    }

    @Override
    public String stripOfMetadata(final String line) {
        return line;
    }

}
