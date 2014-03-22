package com.github.triceo.splitlog.ordering;

import java.util.Comparator;

import com.github.triceo.splitlog.Message;

/**
 * Will sort messages in the order of increasing {@link Message#getUniqueId()}.
 * This will effectively sort them in the order in which they entered the
 * application.
 */
public final class OriginalOrderingMessageComprator implements Comparator<Message> {
    
    public static final Comparator<Message> INSTANCE = new OriginalOrderingMessageComprator();

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
