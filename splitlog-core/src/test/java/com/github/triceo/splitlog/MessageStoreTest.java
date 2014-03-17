package com.github.triceo.splitlog;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MessageStoreTest {

    private static final int NO_MESSAGE_ID = -1;
    private static final int MESSAGE_ID_1 = MessageStoreTest.NO_MESSAGE_ID + 1;
    private static final int MESSAGE_ID_2 = MessageStoreTest.MESSAGE_ID_1 + 1;
    private static final int MESSAGE_ID_3 = MessageStoreTest.MESSAGE_ID_2 + 1;

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
    public void testAutomatedDiscarding() {
        // single-message capacity for easy testing
        final MessageStore store = new MessageStore(1);
        Assert.assertEquals(MessageStoreTest.NO_MESSAGE_ID, store.getFirstMessageId());
        final Message msg1 = new Message("test");
        store.add(msg1);
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_1, store.getFirstMessageId());
        // discarding the first message
        final Message msg2 = new Message("test2");
        store.add(msg2);
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_2, store.getFirstMessageId());
        List<Message> postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_2);
        Assert.assertEquals(1, postDiscard.size());
        Assert.assertSame(msg2, postDiscard.get(0));
        // discarding the second message
        final Message msg3 = new Message("test3");
        store.add(msg3);
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_3, store.getFirstMessageId());
        postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_3);
        Assert.assertEquals(1, postDiscard.size());
        Assert.assertSame(msg3, postDiscard.get(0));
    }

    @Test
    public void testManualDiscarding() {
        // unlimited capacity
        final MessageStore store = new MessageStore();
        Assert.assertEquals(MessageStoreTest.NO_MESSAGE_ID, store.getFirstMessageId());
        final Message msg1 = new Message("test");
        store.add(msg1);
        final Message msg2 = new Message("test2");
        store.add(msg2);
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_1, store.getFirstMessageId());
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_2, store.getLatestMessageId());
        // send third, discard first two
        final Message msg3 = new Message("test3");
        store.add(msg3);
        Assert.assertEquals(2, store.deleteBefore(MessageStoreTest.MESSAGE_ID_3));
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_3, store.getFirstMessageId());
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_3, store.getLatestMessageId());
        final List<Message> postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_3);
        Assert.assertEquals(1, postDiscard.size());
        Assert.assertSame(msg3, postDiscard.get(0));
    }

    @Test
    public void testManualDiscardingOutsideLimits() {
        // unlimited capacity
        final MessageStore store = new MessageStore();
        Assert.assertEquals(MessageStoreTest.NO_MESSAGE_ID, store.getFirstMessageId());
        // ensure proper behavior on empty store
        Assert.assertEquals(0, store.deleteBefore(0)); 
        Assert.assertEquals(0, store.deleteBefore(MESSAGE_ID_1));
        final Message msg1 = new Message("test");
        store.add(msg1);
        final Message msg2 = new Message("test2");
        store.add(msg2);
        // ensure proper behavior when out of bounds
        Assert.assertEquals(0, store.deleteBefore(0));
        Assert.assertEquals(2, store.getAll().size());
        Assert.assertEquals(0, store.deleteBefore(-1));
        Assert.assertEquals(2, store.getAll().size());
        Assert.assertEquals(2, store.deleteBefore(MESSAGE_ID_3));
        Assert.assertEquals(0, store.getAll().size());
        Assert.assertEquals(0, store.deleteBefore(0));
        Assert.assertEquals(0, store.getAll().size());
        Assert.assertEquals(0, store.deleteBefore(-1));
        Assert.assertEquals(0, store.getAll().size());
        Assert.assertEquals(0, store.deleteBefore(1000));
        Assert.assertEquals(0, store.getAll().size());
        // and ensure it still works after we add another
        final Message msg3 = new Message("test3");
        store.add(msg3);
        Assert.assertEquals(1, store.getAll().size());
        Assert.assertEquals(0, store.deleteBefore(0));
        Assert.assertEquals(1, store.getAll().size());
        Assert.assertEquals(0, store.deleteBefore(-1));
        Assert.assertEquals(1, store.getAll().size());
        Assert.assertEquals(1, store.deleteBefore(1000));
        Assert.assertEquals(0, store.getAll().size());
    }
}
