package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.MessageConsumer;
import com.github.triceo.splitlog.api.MessageProducer;

public class MessageManager<P extends MessageProducer<P>> implements MessageProducer<P> {

    @Override
    public boolean isConsuming(final MessageConsumer<P> consumer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean startConsuming(final MessageConsumer<P> consumer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stopConsuming(final MessageConsumer<P> consumer) {
        // TODO Auto-generated method stub
        return false;
    }

}
