package com.github.triceo.splitlog.conditions;

public interface BooleanCondition<T> {

    boolean accept(T evaluate);

}
