package com.github.triceo.splitlog.parsers.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompositePatternPart extends FormattedPatternPart {

    private final List<PatternPart> composited;

    public CompositePatternPart(final List<PatternPart> composited, final Formatting format) {
        super(format);
        this.composited = Collections.unmodifiableList(new ArrayList<PatternPart>(composited));
    }

    public List<PatternPart> getComposited() {
        return this.composited;
    }

}
