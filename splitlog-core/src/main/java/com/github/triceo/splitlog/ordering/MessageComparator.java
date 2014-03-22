package com.github.triceo.splitlog.ordering;

import java.io.Serializable;
import java.util.Comparator;

import com.github.triceo.splitlog.Message;

/**
 * Will be used to compare {@link Message} instances to determine their order.
 */
public interface MessageComparator extends Comparator<Message>, Serializable {

}
