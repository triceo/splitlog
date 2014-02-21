package com.github.triceo.splitlog.exceptions;

class StackTraceLine implements ExceptionLine {

    public enum Source {

        REGULAR, NATIVE, UNKNOWN

    }

    public static StackTraceLine newMethod(final String method, final String className, final int line,
        final String... jarIdentification) {
        return new StackTraceLine(method, className, line, jarIdentification);
    }

    public static StackTraceLine newNativeMethod(final String method, final String... jarIdentification) {
        return new StackTraceLine(method, Source.NATIVE, jarIdentification);
    }

    public static StackTraceLine newUnknownMethod(final String method, final String... jarIdentification) {
        return new StackTraceLine(method, Source.UNKNOWN, jarIdentification);
    }

    private final String methodName, className, classSource, classSourceVersion;

    private final int lineInCode;

    private final Source source;

    private StackTraceLine(final String methodName, final Source source, final String... jarIdentification) {
        this(methodName, null, source, -1, jarIdentification);
    }

    private StackTraceLine(final String methodName, final String className, final int line,
            final String... jarIdentification) {
        this(methodName, className, Source.REGULAR, line, jarIdentification);
    }

    private StackTraceLine(final String methodName, final String className, final Source source, final int line,
            final String... jarIdentification) {
        if ((methodName == null) || (methodName.length() == 0)) {
            throw new IllegalArgumentException("Code must provide method name.");
        } else if ((source == Source.REGULAR) && ((line < 1) || (className == null) || (className.length() == 0))) {
            throw new IllegalArgumentException("Regular code must provide a code line and a class name.");
        } else if ((source != Source.REGULAR) && ((line != -1) || (className != null))) {
            throw new IllegalArgumentException("Native or unknown code must not provide code line or class name.");
        } else if ((jarIdentification.length != 0) && (jarIdentification.length != 2)) {
            throw new IllegalArgumentException("JAR identification, if provided, must have exactly 2 elements.");
        }
        if (jarIdentification.length != 0) {
            this.classSource = jarIdentification[0];
            this.classSourceVersion = jarIdentification[1];
        } else {
            this.classSource = null;
            this.classSourceVersion = null;
        }
        this.className = className;
        this.methodName = methodName;
        this.lineInCode = line;
        this.source = source;
    }

    public String getClassName() {
        return this.className;
    }

    public String getClassSource() {
        return this.classSource;
    }

    public String getClassSourceVersion() {
        return this.classSourceVersion;
    }

    public int getLineInCode() {
        return this.lineInCode;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public Source getSource() {
        return this.source;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("StackTraceLine [");
        if (this.methodName != null) {
            builder.append("methodName=").append(this.methodName).append(", ");
        }
        if (this.className != null) {
            builder.append("className=").append(this.className).append(", ");
        }
        if (this.classSource != null) {
            builder.append("classSource=").append(this.classSource).append(", ");
        }
        if (this.classSourceVersion != null) {
            builder.append("classSourceVersion=").append(this.classSourceVersion).append(", ");
        }
        builder.append("lineInCode=").append(this.lineInCode).append(", ");
        if (this.source != null) {
            builder.append("source=").append(this.source);
        }
        builder.append("]");
        return builder.toString();
    }

}
