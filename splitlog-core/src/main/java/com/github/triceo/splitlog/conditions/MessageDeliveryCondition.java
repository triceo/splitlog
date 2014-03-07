package com.github.triceo.splitlog.conditions;

import com.github.triceo.splitlog.Message;
import com.github.triceo.splitlog.MessageDeliveryStatus;

public interface MessageDeliveryCondition {

    boolean accept(Message evaluate, MessageDeliveryStatus status);

}
