package com.github.triceo.splitlog.parsers.pattern;

public interface PatternPart {

    public static enum ContentType {

        /**
         * Class name.
         */
        FQN(false),
        /**
         * This data is provided by logging system. Although we cannot enumerate
         * the options, we have a way to determine their boundary.
         */
        LOGGER_PROVIDED(false),
        /**
         * This data is provided by logging system, and is likely to contain
         * newline characters. Such as stack trace.
         */
        LOGGER_PROVIDED_MULTILINE(true),
        /**
         * Processing instruction. Possibly ripe for disregarding.
         */
        META(false),
        /**
         * An integer.
         */
        NUMBER(false),
        /**
         * This can be literally anything.
         */
        USER_PROVIDED(false),
        /**
         * This can be literally anything.
         */
        USER_PROVIDED_MULTILINE(true);

        private final boolean possiblyMultiLine;

        ContentType(final boolean mayBeMultiLine) {
            this.possiblyMultiLine = mayBeMultiLine;
        }

        public boolean isPossiblyMultiLine() {
            return this.possiblyMultiLine;
        }
    }

    public ContentType getContentType();

}
