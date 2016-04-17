package com.github.triceo.splitlog.splitters.exceptions;

import java.util.*;

/**
 * A finite automaton for recognizing a Java exception stack trace spanning
 * multiple lines in a random text.
 *
 * Each line has a type. When a line is recognized, a new state is reached. Each
 * line type can only be followed by lines of specific types, together forming a
 * transition function. When the next line is not one of these types, that
 * transition is not in the transition function and the parsing is terminated
 * with an exception. After the last line has been read, if the automaton is not
 * in one of the accepting states, the parsing is terminated with an exception.
 */
final class ExceptionParser {

    private final List<ExceptionLine> parsedLines = new LinkedList<>();

    private static String greatestCommonPrefix(final String a, final CharSequence b) {
        final int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, minLength);
    }

    /**
     * Identifies and removes the common prefix - the longest beginning
     * substring that all the lines share.
     *
     * @param input
     *            Lines to be evaluated.
     * @return The same lines, without the prefix. And stripped of white space
     *         at the ends.
     */
    private static Queue<String> removePrefix(final List<String> input) {
        if (input.size() < 2) {
            return new LinkedList<>(input);
        }
        String resultingPrefix = "";
        final String previousGreatestCommonPrefix = input.get(0).trim();
        String greatestCommonPrefix = "";
        for (int i = 1; i < input.size(); i++) {
            final String previousLine = input.get(i - 1).trim();
            final String currentLine = input.get(i).trim();
            greatestCommonPrefix = ExceptionParser.greatestCommonPrefix(previousLine, currentLine);
            resultingPrefix = ExceptionParser.greatestCommonPrefix(previousGreatestCommonPrefix, greatestCommonPrefix);
            if (resultingPrefix.length() == 0) {
                break;
            }
        }
        final int prefixLength = resultingPrefix.length();
        final boolean hasPrefix = prefixLength > 0;
        final Queue<String> result = new LinkedList<>();
        for (final String line : input) {
            final String line2 = line.trim();
            if (hasPrefix) {
                result.add(line2.substring(prefixLength));
            } else {
                result.add(line2);
            }
        }
        return result;
    }

    /**
     * Browsers through a random log and returns first exception stack trace it
     * could find.
     *
     * @param input Lines of the log. May begin and end with any garbage, may or
     *              may not contain exception.
     * @return Raw exception data.
     * @throws ExceptionParseException If parsing of the log file failed.
     */
    public synchronized Collection<ExceptionLine> parse(final Collection<String> input) throws ExceptionParseException {
        this.parsedLines.clear();
        final Queue<String> linesFromInput = ExceptionParser.removePrefix(new LinkedList<>(input));
        LineType previousLineType = LineType.PRE_START;
        String currentLine = null;
        boolean isFirstLine = true;
        while (!linesFromInput.isEmpty()) {
            currentLine = linesFromInput.poll();
            previousLineType = this.parseLine(previousLineType, currentLine);
            if (isFirstLine) {
                if (!previousLineType.isAcceptableAsFirstLine()) {
                    throw new ExceptionParseException(currentLine, "Invalid line type detected at the beginning: "
                            + previousLineType);
                }
                isFirstLine = false;
            }
        }
        if (!previousLineType.isAcceptableAsLastLine()) {
            throw new ExceptionParseException(currentLine, "Invalid line type detected at the end: " + previousLineType);
        }
        return Collections.unmodifiableCollection(new LinkedList<>(this.parsedLines));
    }

    /**
     * Parse one line in the log.
     *
     * @param previousLineType
     *            Type of the previous line in the log. (The state the automaton
     *            is currently in.)
     * @param line
     *            Line in question.
     * @return Identified type of this line.
     * @throws ExceptionParseException
     *             If parsing of the log file failed.
     */
    private LineType parseLine(final LineType previousLineType, final String line) throws ExceptionParseException {
        switch (previousLineType) {
            case PRE_START:
                return this.parseLine(line, LineType.CAUSE, LineType.PRE_START);
            case CAUSE:
            case SUB_CAUSE:
            case PRE_STACK_TRACE:
                return this.parseLine(line, LineType.STACK_TRACE, LineType.PRE_STACK_TRACE);
            case STACK_TRACE:
                return this.parseLine(line, LineType.STACK_TRACE, LineType.STACK_TRACE_END, LineType.SUB_CAUSE);
            case STACK_TRACE_END:
                return this.parseLine(line, LineType.SUB_CAUSE, LineType.POST_END);
            case POST_END:
                return this.parseLine(line, LineType.POST_END);
            default:
                throw new IllegalArgumentException("Unsupported line type: " + previousLineType);
        }
    }

    /**
     * Parse one line in the log, when knowing the types of lines acceptable at
     * this point in the log.
     *
     * @param line
     *            Line to parse.
     * @param allowedLineTypes
     *            Possible line types, in the order of evaluation. (Possible
     *            transitions.) If evaluation matches for a type, transition
     *            will be made and any subsequent types will be ignored.
     * @return Identified type of this line.
     * @throws ExceptionParseException
     *             If no allowed types match.
     */
    private LineType parseLine(final String line, final LineType... allowedLineTypes) throws ExceptionParseException {
        for (final LineType possibleType : allowedLineTypes) {
            if ((possibleType == LineType.POST_END) || (possibleType == LineType.PRE_START)) {
                // this is garbage, all is accepted without parsing
                return possibleType;
            }
            final ExceptionLine parsedLine = possibleType.parse(line);
            if (parsedLine != null) {
                if (possibleType == LineType.PRE_STACK_TRACE) {
                    // in case the cause is multi-line, update the cause
                    final ExceptionLine currentTop = this.parsedLines.remove(this.parsedLines.size() - 1);
                    if (!((currentTop instanceof CauseLine) && (parsedLine instanceof PlainTextLine))) {
                        throw new IllegalStateException("Garbage in the exception message.");
                    }
                    this.parsedLines.add(new CauseLine((CauseLine) currentTop, (PlainTextLine) parsedLine));
                } else {
                    this.parsedLines.add(parsedLine);
                }
                return possibleType;
            }
        }
        throw new ExceptionParseException(line, "Line not any of the expected types: "
                + Arrays.toString(allowedLineTypes));
    }

    /**
     * Various kinds of states for the parser automaton.
     */
    private enum LineType {
        CAUSE(true, false), POST_END(false, true), PRE_START(true, false), PRE_STACK_TRACE(false, false),
        STACK_TRACE(false, true), STACK_TRACE_END(false, true), SUB_CAUSE(false, false);

        private final boolean mayBeFirstLine, mayBeLastLine;
        private ExceptionLineParser<?> parser;

        LineType(final boolean mayBeginWith, final boolean mayEndWith) {
            this.mayBeFirstLine = mayBeginWith;
            this.mayBeLastLine = mayEndWith;
        }

        private ExceptionLineParser<?> determineParser() {
            switch (this) {
                case PRE_START:
                case POST_END:
                    return null;
                case CAUSE:
                    return new CauseParser();
                case PRE_STACK_TRACE:
                    return new CopyingParser();
                case STACK_TRACE:
                    return new StackTraceParser();
                case STACK_TRACE_END:
                    return new StackTraceEndParser();
                case SUB_CAUSE:
                    return new SubCauseParser();
            }
            throw new IllegalStateException("This can never happen.");
        }

        /**
         * Whether or not this type can be a start state.
         */
        public boolean isAcceptableAsFirstLine() {
            return this.mayBeFirstLine;
        }

        /**
         * Whether or not this type can be an accepting state.
         */
        public boolean isAcceptableAsLastLine() {
            return this.mayBeLastLine;
        }

        /**
         * Parse the line according to this state's parser.
         *
         * @param line Line to parse.
         * @return Parsed line.
         * @throws ExceptionParseException If the parser doesn't recognize the line.
         */
        public ExceptionLine parse(final String line) throws ExceptionParseException {
            if ((this == LineType.POST_END) || (this == LineType.PRE_START)) {
                throw new IllegalStateException("No need to parse garbage lines.");
            } else if (this.parser == null) {
                this.parser = this.determineParser();
            }
            return this.parser.parse(line);
        }
    }

}
