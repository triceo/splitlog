package com.github.triceo.splitlog.parsers.pattern;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class OriginalException extends FormattedPatternPart {

    private final List<String> evaluators;
    private final boolean includePackaging;
    private final boolean rootCauseFirst;
    private final int stackTraceDepth;

    public OriginalException(final Formatting format, final boolean includePackaging, final boolean rootCauseFirst) {
        this(format, includePackaging, rootCauseFirst, -1);
    }

    public OriginalException(final Formatting format, final boolean includePackaging, final boolean rootCauseFirst,
            final int depthOfStackTrace) {
        this(format, includePackaging, rootCauseFirst, depthOfStackTrace, Collections.<String> emptyList());
    }

    public OriginalException(final Formatting format, final boolean includePackaging, final boolean rootCauseFirst,
        final int depthOfStackTrace, final List<String> evaluators) {
        super(format, ContentType.LOGGER_PROVIDED);
        this.stackTraceDepth = depthOfStackTrace;
        this.includePackaging = includePackaging;
        this.rootCauseFirst = rootCauseFirst;
        this.evaluators = Collections.unmodifiableList(new LinkedList<String>(evaluators));
    }

    public List<String> getEvaluators() {
        return this.evaluators;
    }

    public int getStackTraceDepth() {
        return this.stackTraceDepth;
    }

    public boolean isIncludePackaging() {
        return this.includePackaging;
    }

    public boolean isRootCauseFirst() {
        return this.rootCauseFirst;
    }

}
