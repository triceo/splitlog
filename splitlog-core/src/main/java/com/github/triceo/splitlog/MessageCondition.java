package com.github.triceo.splitlog;

public interface MessageCondition {

    boolean accept(Message msg);

}
