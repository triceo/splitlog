package com.github.triceo.splitlog.parsers.pattern;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Caller extends FormattedPatternPart {

    private final List<String> evaluators;
    private final int stackTraceDepth;

    public Caller(final Formatting format) {
        this(format, -1);
    }

    public Caller(final Formatting format, final int depthOfStackTrace) {
        this(format, depthOfStackTrace, Collections.<String> emptyList());
    }

    public Caller(final Formatting format, final int depthOfStackTrace, final List<String> evaluators) {
        super(format, ContentType.LOGGER_PROVIDED);
        this.stackTraceDepth = depthOfStackTrace;
        this.evaluators = Collections.unmodifiableList(new LinkedList<String>(evaluators));
    }

    public List<String> getEvaluators() {
        return this.evaluators;
    }

    public int getStackTraceDepth() {
        return this.stackTraceDepth;
    }

}
