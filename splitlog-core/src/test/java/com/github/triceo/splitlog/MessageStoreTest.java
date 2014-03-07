package com.github.triceo.splitlog;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MessageStoreTest {

    @Test
    public void testAdditions() {
        final MessageStore store = new MessageStore();
        Assert.assertEquals(0, store.getNextMessageId());
        Assert.assertEquals(-1, store.getLatestMessageId());
        store.add(new Message("Test"));
        Assert.assertEquals(1, store.getNextMessageId());
        Assert.assertEquals(0, store.getLatestMessageId());
        store.add(new Message("Test2"));
        Assert.assertEquals(2, store.getNextMessageId());
        Assert.assertEquals(1, store.getLatestMessageId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRangeOnEmpty1() {
        final MessageStore store = new MessageStore();
        store.getFromRange(0, 1);
    }

    @Test
    public void testValidRange() {
        final MessageStore store = new MessageStore();
        // add first message
        final Message msg = new Message("test");
        store.add(msg);
        List<Message> range = store.getFromRange(0, 1);
        Assert.assertEquals(1, range.size());
        Assert.assertSame(msg, range.get(0));
        // add second message
        final Message msg2 = new Message("test2");
        store.add(msg2);
        range = store.getFromRange(1, 2);
        Assert.assertEquals(1, range.size());
        Assert.assertSame(msg2, range.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange1() {
        final MessageStore store = new MessageStore();
        store.add(new Message("test"));
        store.add(new Message("test2"));
        store.getFromRange(0, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange2() {
        final MessageStore store = new MessageStore();
        store.add(new Message("test"));
        store.add(new Message("test2"));
        store.getFromRange(1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange3() {
        final MessageStore store = new MessageStore();
        store.add(new Message("test"));
        store.add(new Message("test2"));
        store.getFromRange(1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange4() {
        final MessageStore store = new MessageStore();
        store.add(new Message("test"));
        store.add(new Message("test2"));
        store.getFromRange(-1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRangeOnEmpty2() {
        final MessageStore store = new MessageStore();
        store.getFromRange(0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeRange1() {
        final MessageStore store = new MessageStore();
        store.getFromRange(0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeRange2() {
        final MessageStore store = new MessageStore();
        store.getFromRange(-1, 0);
    }

    @Test
    public void testDiscarding() {
        final int NO_MESSAGE_ID = -1;
        final int MESSAGE_ID_1 = NO_MESSAGE_ID + 1;
        final int MESSAGE_ID_2 = MESSAGE_ID_1 + 1;
        final int MESSAGE_ID_3 = MESSAGE_ID_2 + 1;
        // single-message capacity for easy testing
        final MessageStore store = new MessageStore(1);
        Assert.assertEquals(NO_MESSAGE_ID, store.getFirstMessageId());
        final Message msg1 = new Message("test");
        store.add(msg1);
        Assert.assertEquals(MESSAGE_ID_1, store.getFirstMessageId());
        // discarding the first message
        final Message msg2 = new Message("test2");
        store.add(msg2);
        Assert.assertEquals(MESSAGE_ID_2, store.getFirstMessageId());
        List<Message> postDiscard = store.getFrom(MESSAGE_ID_2);
        Assert.assertEquals(1, postDiscard.size());
        Assert.assertSame(msg2, postDiscard.get(0));
        // discarding the second message
        final Message msg3 = new Message("test3");
        store.add(msg3);
        Assert.assertEquals(MESSAGE_ID_3, store.getFirstMessageId());
        postDiscard = store.getFrom(MESSAGE_ID_3);
        Assert.assertEquals(1, postDiscard.size());
        Assert.assertSame(msg3, postDiscard.get(0));
    }

}
