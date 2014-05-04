package com.github.triceo.splitlog.parsers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.triceo.splitlog.parsers.pattern.Literal;
import com.github.triceo.splitlog.parsers.pattern.PatternPart;

public final class SplitlogPatternParser {

    public static final char NEWLINE = '\n';

    public static SplitlogPatternParser build(final PatternTranslator translator, final String pattern) {
        return new SplitlogPatternParser(translator.parse(pattern));
    }

    private final StringBuilder lines = new StringBuilder();
    private final Map<Pair<Literal, Literal>, List<PatternPart>> literalSeparatedPatternParts;

    private SplitlogPatternParser(final List<PatternPart> parts) {
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Empty patterns are useless.");
        }
        final SortedMap<Integer, Literal> literalPositions = new TreeMap<Integer, Literal>();
        // identify literal positions
        for (int i = 0; i < parts.size(); i++) {
            final PatternPart part = parts.get(i);
            if (part instanceof Literal) {
                literalPositions.put(i, (Literal) part);
            }
        }
        // split pattern parts by the literals
        this.literalSeparatedPatternParts = new LinkedHashMap<Pair<Literal, Literal>, List<PatternPart>>();
        final int firstKey = literalPositions.firstKey();
        if (firstKey > 0) { // pattern doesn't start with a literal
            final Literal firstLiteral = null;
            final Literal secondLiteral = literalPositions.get(firstKey);
            this.literalSeparatedPatternParts.put(ImmutablePair.of(firstLiteral, secondLiteral),
                    parts.subList(0, firstKey));
        }
        while (literalPositions.size() > 1) { // patterns in the middle
            final int literalPosition = literalPositions.firstKey();
            final Literal literal = literalPositions.remove(literalPosition);
            final int secondLiteralPosition = literalPositions.firstKey();
            final Literal secondLiteral = literalPositions.get(secondLiteralPosition);
            this.literalSeparatedPatternParts.put(ImmutablePair.of(literal, secondLiteral),
                    parts.subList(literalPosition + 1, secondLiteralPosition));
        }
        final int lastKey = literalPositions.lastKey();
        if (lastKey != (parts.size() - 1)) { // no literal at the end
            final Literal firstLiteral = literalPositions.get(lastKey);
            final Literal secondLiteral = null;
            this.literalSeparatedPatternParts.put(ImmutablePair.of(firstLiteral, secondLiteral),
                    parts.subList(lastKey + 1, parts.size()));
        }
    }

    public void addLine(final String line) {
        // add line to the existing message
        if (line.contains(String.valueOf(SplitlogPatternParser.NEWLINE))) {
            throw new IllegalArgumentException("Cannot accept multi-line strings.");
        }
        this.lines.append(line).append(SplitlogPatternParser.NEWLINE);
        // invoke pattern parsing
    }
}
