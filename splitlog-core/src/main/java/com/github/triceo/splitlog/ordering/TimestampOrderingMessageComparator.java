package com.github.triceo.splitlog.ordering;

import java.util.Comparator;

import com.github.triceo.splitlog.Message;

/**
 * Will sort messages in the order of increasing {@link Message#getDate()}.
 */
public final class TimestampOrderingMessageComparator implements Comparator<Message> {

    public static final Comparator<Message> INSTANCE = new TimestampOrderingMessageComparator();

    private TimestampOrderingMessageComparator() {
        // singleton
    }

    @Override
    public int compare(final Message o1, final Message o2) {
        return o1.getDate().compareTo(o2.getDate());
    }

}
