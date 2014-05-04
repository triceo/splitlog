package com.github.triceo.splitlog.parsers.logback;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import ch.qos.logback.core.pattern.FormatInfo;
import ch.qos.logback.core.pattern.parser.SimpleKeywordNode;

import com.github.triceo.splitlog.parsers.SplitlogPatternParser;
import com.github.triceo.splitlog.parsers.pattern.Caller;
import com.github.triceo.splitlog.parsers.pattern.ContextName;
import com.github.triceo.splitlog.parsers.pattern.Diagnostic;
import com.github.triceo.splitlog.parsers.pattern.FormattedPatternPart;
import com.github.triceo.splitlog.parsers.pattern.Formatting;
import com.github.triceo.splitlog.parsers.pattern.Formatting.Padding;
import com.github.triceo.splitlog.parsers.pattern.Formatting.Truncate;
import com.github.triceo.splitlog.parsers.pattern.IssuingClass;
import com.github.triceo.splitlog.parsers.pattern.Level;
import com.github.triceo.splitlog.parsers.pattern.Line;
import com.github.triceo.splitlog.parsers.pattern.Literal;
import com.github.triceo.splitlog.parsers.pattern.LoggingDate;
import com.github.triceo.splitlog.parsers.pattern.Marker;
import com.github.triceo.splitlog.parsers.pattern.NopException;
import com.github.triceo.splitlog.parsers.pattern.OriginMethod;
import com.github.triceo.splitlog.parsers.pattern.OriginThread;
import com.github.triceo.splitlog.parsers.pattern.OriginalException;
import com.github.triceo.splitlog.parsers.pattern.OriginalLogger;
import com.github.triceo.splitlog.parsers.pattern.PatternPart;
import com.github.triceo.splitlog.parsers.pattern.Payload;
import com.github.triceo.splitlog.parsers.pattern.Property;
import com.github.triceo.splitlog.parsers.pattern.Relative;
import com.github.triceo.splitlog.parsers.pattern.SourceFile;

public enum ConversionWord {
    CALLER("caller") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            final Formatting f = this.getFormatting(node);
            final List<String> options = this.getOptions(node);
            switch (options.size()) {
                case 0:
                    return new Caller(f);
                case 1:
                    return new Caller(f, Integer.valueOf(options.get(0)));
                default:
                    return new Caller(f, Integer.valueOf(options.get(0)), options.subList(1, options.size()));
            }
        }
    },
    CONTEXT_NAME("contextName", "cn") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new ContextName(this.getFormatting(node));
        }
    },
    DATE("d", "date") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            final Formatting format = this.getFormatting(node);
            final List<String> options = this.getOptions(node);
            switch (options.size()) {
                case 0:
                    return new LoggingDate(format);
                case 1:
                    return new LoggingDate(format, options.get(0));
                case 2:
                    return new LoggingDate(format, options.get(0), options.get(1));
                default:
                    throw new IllegalStateException("LoggingDate can not have more than 2 options.");
            }
        }
    },
    FILE("F", "file") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new SourceFile(this.getFormatting(node));
        }
    },
    ISSUING_CLASS("C", "class") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            final Formatting format = this.getFormatting(node);
            final List<String> options = this.getOptions(node);
            switch (options.size()) {
                case 0:
                    return new IssuingClass(format);
                case 1:
                    return new IssuingClass(format, Integer.valueOf(options.get(0)));
                default:
                    throw new IllegalStateException("Issuing class can not have more than 1 option.");
            }
        }
    },
    LEVEL("p", "le", "level") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new Level(this.getFormatting(node));
        }
    },
    LINE("L", "line") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new Line(this.getFormatting(node));
        }
    },
    MARKER("marker") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new Marker(this.getFormatting(node));
        }
    },
    MDC("X", "mdc") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            final Formatting format = this.getFormatting(node);
            final List<String> options = this.getOptions(node);
            switch (options.size()) {
                case 0:
                    return new Diagnostic(format);
                case 1:
                    final String[] parts = options.get(0).split("\\Q:-\\E");
                    if (parts.length == 1) {
                        return new Diagnostic(format, parts[0]);
                    } else if (parts.length == 2) {
                        return new Diagnostic(format, parts[0], parts[1]);
                    } else {
                        throw new IllegalStateException("Invalid option format: " + Arrays.toString(parts));
                    }
                default:
                    throw new IllegalStateException("Issuing class can not have more than 1 option.");
            }
        }
    },
    MESSAGE("m", "msg", "message") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new Payload(this.getFormatting(node));
        }
    },
    METHOD("M", "method") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new OriginMethod(this.getFormatting(node));
        }
    },
    NEWLINE("n") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new Literal(SplitlogPatternParser.NEWLINE);
        }
    },
    NO_EXCEPTION("nopex", "nopexception") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new NopException(this.getFormatting(node));
        }
    },
    ORIGIN("c", "lo", "logger") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            final Formatting format = this.getFormatting(node);
            final List<String> options = this.getOptions(node);
            switch (options.size()) {
                case 0:
                    return new OriginalLogger(format);
                case 1:
                    return new OriginalLogger(format, Integer.valueOf(options.get(0)));
                default:
                    throw new IllegalStateException("OriginalLogger can not have more than 1 option.");
            }
        }
    },
    PROPERTY("property") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            final String option = this.assertOneOption(node);
            return new Property(this.getFormatting(node), option);
        }
    },
    RELATIVE("r", "relative") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new Relative(this.getFormatting(node));
        }
    },
    THREAD("t", "thread") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            this.assertNoOptions(node);
            return new OriginThread(this.getFormatting(node));
        }
    },
    THROWABLE("ex", "exception", "throwable") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            return this.processException(node, false, false);
        }
    },
    THROWABLE_ROOT_FIRST("rEx", "rException", "rThrowable") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            return this.processException(node, true, true);
        }
    },
    THROWABLE_WITH_PACKAGING("xEx", "xException", "xThrowable") {
        @Override
        public PatternPart convert(final SimpleKeywordNode node) {
            return this.processException(node, true, false);
        }
    };

    private final Collection<String> aliases = new TreeSet<String>();

    ConversionWord(final String... aliases) {
        for (final String alias : aliases) {
            this.aliases.add(alias);
        }
    }

    protected void assertNoOptions(final SimpleKeywordNode node) {
        if (this.getOptions(node).size() > 0) {
            throw new IllegalStateException("Node cannot have any options: " + node);
        }
    }

    protected String assertOneOption(final SimpleKeywordNode node) {
        final List<String> options = this.getOptions(node);
        if (options.size() != 1) {
            throw new IllegalStateException("Node must have 1 option: " + node);
        }
        return options.get(0);
    }

    public abstract PatternPart convert(SimpleKeywordNode node);

    public Collection<String> getAliases() {
        return this.aliases;
    }

    protected int getExceptionLength(final String length) {
        if (length.equals("full")) {
            return -1;
        } else if (length.equals("short")) {
            return 1;
        } else {
            return Integer.valueOf(length);
        }
    }

    protected Formatting getFormatting(final SimpleKeywordNode node) {
        final FormatInfo fi = node.getFormatInfo();
        if (fi == null) {
            return new Formatting();
        } else {
            return new Formatting(fi.getMin(), fi.getMax(), fi.isLeftPad() ? Padding.LEFT : Padding.RIGHT,
                    fi.isLeftTruncate() ? Truncate.BEGINNING : Truncate.END);
        }
    }

    protected List<String> getOptions(final SimpleKeywordNode node) {
        final List<String> result = new LinkedList<String>();
        final List<String> options = node.getOptions();
        if ((options == null) || (options.size() == 0)) {
            return Collections.emptyList();
        }
        result.addAll(options);
        return Collections.unmodifiableList(result);
    }

    protected FormattedPatternPart processException(final SimpleKeywordNode node, final boolean includePackaging,
        final boolean rootCauseFirst) {
        final Formatting f = this.getFormatting(node);
        final List<String> options = this.getOptions(node);
        switch (options.size()) {
            case 0:
                return new OriginalException(f, includePackaging, rootCauseFirst);
            case 1:
                return new OriginalException(f, includePackaging, rootCauseFirst, this.getExceptionLength(options
                        .get(0)));
            default:
                return new OriginalException(f, includePackaging, rootCauseFirst, this.getExceptionLength(options
                        .get(0)), options.subList(1, options.size()));
        }
    }
}