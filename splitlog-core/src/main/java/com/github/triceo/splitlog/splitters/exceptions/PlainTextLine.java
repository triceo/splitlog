package com.github.triceo.splitlog.splitters.exceptions;

class PlainTextLine implements ExceptionLine {

    private final String text;

    public PlainTextLine(final String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return "PlainTextLine [text=" + this.text + "]";
    }

}
