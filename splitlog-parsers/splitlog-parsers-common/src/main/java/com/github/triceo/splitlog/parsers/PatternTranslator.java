package com.github.triceo.splitlog.parsers;

import java.util.List;

import com.github.triceo.splitlog.parsers.pattern.PatternPart;

public interface PatternTranslator {

    List<PatternPart> parse(String pattern);

}
