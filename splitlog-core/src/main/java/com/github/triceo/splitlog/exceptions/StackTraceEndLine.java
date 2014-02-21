package com.github.triceo.splitlog.exceptions;

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
        final StringBuilder builder = new StringBuilder();
        builder.append("StackTraceEndLine [howManyOmmitted=").append(this.howManyOmmitted).append("]");
        return builder.toString();
    }

}
