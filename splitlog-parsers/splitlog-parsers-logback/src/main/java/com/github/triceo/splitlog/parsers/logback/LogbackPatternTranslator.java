package com.github.triceo.splitlog.parsers.logback;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.qos.logback.core.pattern.FormatInfo;
import ch.qos.logback.core.pattern.parser.CompositeNode;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.pattern.parser.SimpleKeywordNode;
import ch.qos.logback.core.spi.ScanException;

import com.github.triceo.splitlog.parsers.pattern.CompositePatternPart;
import com.github.triceo.splitlog.parsers.pattern.FormattedPatternPart;
import com.github.triceo.splitlog.parsers.pattern.Formatting;
import com.github.triceo.splitlog.parsers.pattern.Formatting.Padding;
import com.github.triceo.splitlog.parsers.pattern.Formatting.Truncate;
import com.github.triceo.splitlog.parsers.pattern.Literal;
import com.github.triceo.splitlog.parsers.pattern.PatternPart;

public class LogbackPatternTranslator {

    private static final Map<String, Parser<CompositeNode>> PARSERS = new LinkedHashMap<String, Parser<CompositeNode>>();

    private static CompositePatternPart convertCompositeNode(final CompositeNode n) {
        final FormatInfo fi = n.getFormatInfo();
        final Formatting f = fi == null ? new Formatting() : new Formatting(fi.getMin(), fi.getMax(), fi.isLeftPad() ? Padding.LEFT : Padding.RIGHT,
                fi.isLeftTruncate() ? Truncate.BEGINNING : Truncate.END);
        return new CompositePatternPart(LogbackPatternTranslator.inlineNode(n.getChildNode()), f);
    }

    private static PatternPart convertNode(final Node n) {
        switch (n.getType()) {
            case 0:
                return new Literal((String) n.getValue());
            case 1:
                return LogbackPatternTranslator.convertSimpleKeywordNode((SimpleKeywordNode) n);
            case 2:
                return LogbackPatternTranslator.convertCompositeNode((CompositeNode) n);
            default:
                throw new IllegalArgumentException("Unknown node type: " + n);
        }
    }

    private static FormattedPatternPart convertSimpleKeywordNode(final SimpleKeywordNode n) {
        for (final ConversionWord w : ConversionWord.values()) {
            if (w.getAliases().contains(n.getValue())) {
                return w.convert(n);
            }
        }
        throw new IllegalStateException("Unknown node type: " + n);
    }

    private static Parser<CompositeNode> getParser(final String pattern) throws ScanException {
        if (!LogbackPatternTranslator.PARSERS.containsKey(pattern)) {
            final Parser<CompositeNode> p = new Parser<CompositeNode>(pattern);
            LogbackPatternTranslator.PARSERS.put(pattern, p);
        }
        return LogbackPatternTranslator.PARSERS.get(pattern);
    }

    private static List<PatternPart> inlineNode(final Node node) {
        final List<PatternPart> nodes = new LinkedList<PatternPart>();
        Node n = node;
        while (n != null) {
            nodes.add(LogbackPatternTranslator.convertNode(n));
            n = n.getNext();
        }
        return nodes;
    }

    public List<PatternPart> parse(final String pattern) {
        try {
            final Parser<CompositeNode> parser = LogbackPatternTranslator.getParser(pattern);
            return LogbackPatternTranslator.inlineNode(parser.parse());
        } catch (final ScanException e) {
            // FIXME parser instantiation should happen at a different place?
            throw new IllegalStateException("Failed parsing.", e);
        }
    }
}
