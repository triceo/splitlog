package com.github.triceo.splitlog.splitters;

import java.util.Calendar;
import java.util.Date;

import com.github.triceo.splitlog.MessageBuilder;
import com.github.triceo.splitlog.MessageSeverity;
import com.github.triceo.splitlog.MessageType;

final public class SimpleTailSplitter extends AbstractTailSplitter {

    @Override
    public Date determineDate(final MessageBuilder message) {
        return Calendar.getInstance().getTime();
    }

    @Override
    public MessageSeverity determineSeverity(final MessageBuilder message) {
        return MessageSeverity.UNKNOWN;
    }

    @Override
    public MessageType determineType(final MessageBuilder message) {
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
