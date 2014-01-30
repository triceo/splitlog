package com.github.triceo.splitlog;

import java.util.List;

class NonStoringLogTailer extends AbstractLogTailer {

    public NonStoringLogTailer(final LogWatch watch) {
        super(watch);
    }

    @Override
    public List<Message> getMessages() {
        return this.getWatch().getAllMessages(this);
    }

    @Override
    protected void notifyOfMessage(final Message msg) {
        // we are not doing anything to the messages
    }

}
