package com.github.triceo.splitlog.ordering;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageComparator;

/**
 * Will sort messages in the order of increasing {@link Message#getUniqueId()}.
 * This will effectively sort them in the order in which they entered the
 * application.
 */
public final class OriginalOrderingMessageComprator implements MessageComparator {

    public static final MessageComparator INSTANCE = new OriginalOrderingMessageComprator();
    private static final long serialVersionUID = 4745195072835417880L;

    private OriginalOrderingMessageComprator() {
        // singleton
    }

    @Override
    public int compare(final Message o1, final Message o2) {
        return o1.compareTo(o2);
    }

}
