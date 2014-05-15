package com.github.triceo.splitlog;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.LogWatchBuilder;

@RunWith(Parameterized.class)
public class SimpleMessageWritingTest extends AbstractMessageWritingTest {

    public SimpleMessageWritingTest(final LogWatchBuilder builder) {
        super(builder);
    }

    @Override
    public void testWriteMessages() {
        this.writeAndTest(false);
    }

}
