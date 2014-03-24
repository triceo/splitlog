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
        Assert.assertEquals(0, store.getNextPosition());
        Assert.assertEquals(-1, store.getLatestPosition());
        store.add(new MessageBuilder("Test").buildFinal());
        Assert.assertEquals(1, store.getNextPosition());
        Assert.assertEquals(0, store.getLatestPosition());
        store.add(new MessageBuilder("Test2").buildFinal());
        Assert.assertEquals(2, store.getNextPosition());
        Assert.assertEquals(1, store.getLatestPosition());
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
        final Message msg = new MessageBuilder("test").buildFinal();
        store.add(msg);
        List<Message> range = store.getFromRange(0, 1);
        Assert.assertEquals(1, range.size());
        Assert.assertSame(msg, range.get(0));
        // add second message
        final Message msg2 = new MessageBuilder("test2").buildFinal();
        store.add(msg2);
        range = store.getFromRange(1, 2);
        Assert.assertEquals(1, range.size());
        Assert.assertSame(msg2, range.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange1() {
        final MessageStore store = new MessageStore();
        store.add(new MessageBuilder("test").buildFinal());
        store.add(new MessageBuilder("test2").buildFinal());
        store.getFromRange(0, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange2() {
        final MessageStore store = new MessageStore();
        store.add(new MessageBuilder("test").buildFinal());
        store.add(new MessageBuilder("test2").buildFinal());
        store.getFromRange(1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange3() {
        final MessageStore store = new MessageStore();
        store.add(new MessageBuilder("test").buildFinal());
        store.add(new MessageBuilder("test2").buildFinal());
        store.getFromRange(1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange4() {
        final MessageStore store = new MessageStore();
        store.add(new MessageBuilder("test").buildFinal());
        store.add(new MessageBuilder("test2").buildFinal());
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
        Assert.assertEquals(MessageStoreTest.NO_MESSAGE_ID, store.getFirstPosition());
        final Message msg1 = new MessageBuilder("test").buildFinal();
        store.add(msg1);
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_1, store.getFirstPosition());
        // discarding the first message
        final Message msg2 = new MessageBuilder("test2").buildFinal();
        store.add(msg2);
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_2, store.getFirstPosition());
        List<Message> postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_2);
        Assert.assertEquals(1, postDiscard.size());
        Assert.assertSame(msg2, postDiscard.get(0));
        // discarding the second message
        final Message msg3 = new MessageBuilder("test3").buildFinal();
        store.add(msg3);
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_3, store.getFirstPosition());
        postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_3);
        Assert.assertEquals(1, postDiscard.size());
        Assert.assertSame(msg3, postDiscard.get(0));
    }

    @Test
    public void testManualDiscarding() {
        // unlimited capacity
        final MessageStore store = new MessageStore();
        Assert.assertEquals(MessageStoreTest.NO_MESSAGE_ID, store.getFirstPosition());
        final Message msg1 = new MessageBuilder("test").buildFinal();
        store.add(msg1);
        final Message msg2 = new MessageBuilder("test2").buildFinal();
        store.add(msg2);
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_1, store.getFirstPosition());
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_2, store.getLatestPosition());
        // send third, discard first two
        final Message msg3 = new MessageBuilder("test3").buildFinal();
        store.add(msg3);
        Assert.assertEquals(2, store.discardBefore(MessageStoreTest.MESSAGE_ID_3));
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_3, store.getFirstPosition());
        Assert.assertEquals(MessageStoreTest.MESSAGE_ID_3, store.getLatestPosition());
        final List<Message> postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_3);
        Assert.assertEquals(1, postDiscard.size());
        Assert.assertSame(msg3, postDiscard.get(0));
    }

    @Test
    public void testManualDiscardingOutsideLimits() {
        // unlimited capacity
        final MessageStore store = new MessageStore();
        Assert.assertEquals(MessageStoreTest.NO_MESSAGE_ID, store.getFirstPosition());
        // ensure proper behavior on empty store
        Assert.assertEquals(0, store.discardBefore(0));
        Assert.assertEquals(0, store.discardBefore(MessageStoreTest.MESSAGE_ID_1));
        final Message msg1 = new MessageBuilder("test").buildFinal();
        store.add(msg1);
        final Message msg2 = new MessageBuilder("test2").buildFinal();
        store.add(msg2);
        // ensure proper behavior when out of bounds
        Assert.assertEquals(0, store.discardBefore(0));
        Assert.assertEquals(2, store.getAll().size());
        Assert.assertEquals(0, store.discardBefore(-1));
        Assert.assertEquals(2, store.getAll().size());
        Assert.assertEquals(2, store.discardBefore(MessageStoreTest.MESSAGE_ID_3));
        Assert.assertEquals(0, store.getAll().size());
        Assert.assertEquals(0, store.discardBefore(0));
        Assert.assertEquals(0, store.getAll().size());
        Assert.assertEquals(0, store.discardBefore(-1));
        Assert.assertEquals(0, store.getAll().size());
        Assert.assertEquals(0, store.discardBefore(1000));
        Assert.assertEquals(0, store.getAll().size());
        // and ensure it still works after we add another
        final Message msg3 = new MessageBuilder("test3").buildFinal();
        store.add(msg3);
        Assert.assertEquals(1, store.getAll().size());
        Assert.assertEquals(0, store.discardBefore(0));
        Assert.assertEquals(1, store.getAll().size());
        Assert.assertEquals(0, store.discardBefore(-1));
        Assert.assertEquals(1, store.getAll().size());
        Assert.assertEquals(1, store.discardBefore(1000));
        Assert.assertEquals(0, store.getAll().size());
    }
}
