package com.github.triceo.splitlog;

public interface BooleanCondition<T> {

    public boolean accept(T evaluate);

}
