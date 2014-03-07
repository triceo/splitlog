package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.Message;

public interface MessageCondition {

    boolean accept(Message evaluate);

}
