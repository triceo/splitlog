package com.github.triceo.splitlog;

public interface MessageClassifier<T> {

    T classify(RawMessage m);
    
}
