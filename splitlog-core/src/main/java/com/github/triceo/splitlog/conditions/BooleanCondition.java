package com.github.triceo.splitlog.conditions;

public interface BooleanCondition<T> {

    public boolean accept(T evaluate);

}
