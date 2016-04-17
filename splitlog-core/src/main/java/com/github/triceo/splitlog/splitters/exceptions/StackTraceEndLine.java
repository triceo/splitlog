package com.github.triceo.splitlog.splitters.exceptions;

class StackTraceEndLine implements ExceptionLine {

    private final int howManyOmmitted;

    public StackTraceEndLine(final int howManyOmmitted) {
        this.howManyOmmitted = howManyOmmitted;
    }

    public int getHowManyOmmitted() {
        return this.howManyOmmitted;
    }

    @Override
    public String toString() {
        return "StackTraceEndLine [howManyOmmitted=" + this.howManyOmmitted + "]";
    }

}
