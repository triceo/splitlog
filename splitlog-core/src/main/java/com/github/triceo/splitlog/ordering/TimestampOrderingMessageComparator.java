package com.github.triceo.splitlog.ordering;

import com.github.triceo.splitlog.Message;

/**
 * Will sort messages in the order of increasing {@link Message#getDate()}.
 */
public final class TimestampOrderingMessageComparator implements MessageComparator {

    private static final long serialVersionUID = 810642571483072218L;
    public static final MessageComparator INSTANCE = new TimestampOrderingMessageComparator();

    private TimestampOrderingMessageComparator() {
        // singleton
    }

    @Override
    public int compare(final Message o1, final Message o2) {
        return o1.getDate().compareTo(o2.getDate());
    }

}
