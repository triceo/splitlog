package com.github.triceo.splitlog.splitters.exceptions;

class CopyingParser extends ExceptionLineParser<PlainTextLine> {

    @Override
    public PlainTextLine parse(final String line) {
        return new PlainTextLine(line);
    }

}
