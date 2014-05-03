package com.github.triceo.splitlog.parsers.pattern;

public interface PatternPart {

    public static enum ContentType {

        /**
         * Class name.
         */
        FQN,
        /**
         * This data is provided by logging system. Although we cannot enumerate
         * the options, we have a way to determine their boundary.
         */
        LOGGER_PROVIDED,
        /**
         * Processing instruction. Possibly ripe for disregarding.
         */
        META,
        /**
         * An integer.
         */
        NUMBER,
        /**
         * This can be literally anything.
         */
        USER_PROVIDED;
    }

    public ContentType getContentType();

}
