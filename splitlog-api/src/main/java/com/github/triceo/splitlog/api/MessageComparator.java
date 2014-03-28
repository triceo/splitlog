package com.github.triceo.splitlog.api;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Will be used to compare {@link Message} instances to determine their order.
 */
public interface MessageComparator extends Comparator<Message>, Serializable {

}
