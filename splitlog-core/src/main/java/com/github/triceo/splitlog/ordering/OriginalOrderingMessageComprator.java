package com.github.triceo.splitlog.ordering;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;

/**
 * Will sort messages in the order of increasing {@link Message#getUniqueId()}.
 * This will effectively sort them in the order in which they entered the
 * application.
 */
public final class OriginalOrderingMessageComprator implements MessageComparator {

    private static final long serialVersionUID = 4745195072835417880L;
    public static final MessageComparator INSTANCE = new OriginalOrderingMessageComprator();

    private OriginalOrderingMessageComprator() {
        // singleton
    }

    @Override
    public int compare(final Message o1, final Message o2) {
        if (o1.getUniqueId() == o2.getUniqueId()) {
            return 0;
        } else if (o1.getUniqueId() < o2.getUniqueId()) {
            return -1;
        } else {
            return 1;
        }
    }

}
