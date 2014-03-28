package com.github.triceo.splitlog.splitters.exceptions;

class CauseLine implements ExceptionLine {

    private final String className, message;

    public CauseLine(final String className, final String message) {
        this.className = className;
        this.message = message;
    }

    public String getClassName() {
        return this.className;
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return "CauseLine [className=" + this.className + ", message=" + this.message + "]";
    }

}
