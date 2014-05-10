package com.github.triceo.splitlog;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Message;

public class MessageStoreTest extends AbstractSplitlogTest {

    private static final int MESSAGE_ID_1 = MessageStoreTest.NO_MESSAGE_ID + 1;
    private static final int MESSAGE_ID_2 = MessageStoreTest.MESSAGE_ID_1 + 1;
    private static final int MESSAGE_ID_3 = MessageStoreTest.MESSAGE_ID_2 + 1;
    private static final int NO_MESSAGE_ID = -1;

    @Test
    public void testAdditions() {
        final MessageStore store = new MessageStore();
        Assertions.assertThat(store.getNextPosition()).isEqualTo(0);
        Assertions.assertThat(store.getLatestPosition()).isEqualTo(-1);
        store.add(new MessageBuilder("Test").buildFinal());
        Assertions.assertThat(store.getNextPosition()).isEqualTo(1);
        Assertions.assertThat(store.getLatestPosition()).isEqualTo(0);
        store.add(new MessageBuilder("Test2").buildFinal());
        Assertions.assertThat(store.getNextPosition()).isEqualTo(2);
        Assertions.assertThat(store.getLatestPosition()).isEqualTo(1);
    }

    @Test
    public void testAutomatedDiscarding() {
        // single-message capacity for easy testing
        final MessageStore store = new MessageStore(1);
        Assertions.assertThat(store.getFirstPosition()).isEqualTo(MessageStoreTest.NO_MESSAGE_ID);
        final Message msg1 = new MessageBuilder("test").buildFinal();
        store.add(msg1);
        Assertions.assertThat(store.getFirstPosition()).isEqualTo(MessageStoreTest.MESSAGE_ID_1);
        // discarding the first message
        final Message msg2 = new MessageBuilder("test2").buildFinal();
        store.add(msg2);
        Assertions.assertThat(store.getFirstPosition()).isEqualTo(MessageStoreTest.MESSAGE_ID_2);
        List<Message> postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_2);
        Assertions.assertThat(postDiscard.size()).isEqualTo(1);
        Assertions.assertThat(postDiscard.get(0)).isSameAs(msg2);
        // discarding the second message
        final Message msg3 = new MessageBuilder("test3").buildFinal();
        store.add(msg3);
        Assertions.assertThat(store.getFirstPosition()).isEqualTo(MessageStoreTest.MESSAGE_ID_3);
        postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_3);
        Assertions.assertThat(postDiscard.size()).isEqualTo(1);
        Assertions.assertThat(postDiscard.get(0)).isSameAs(msg3);
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
    public void testInvalidRangeOnEmpty1() {
        final MessageStore store = new MessageStore();
        store.getFromRange(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRangeOnEmpty2() {
        final MessageStore store = new MessageStore();
        store.getFromRange(0, 0);
    }

    @Test
    public void testManualDiscarding() {
        // unlimited capacity
        final MessageStore store = new MessageStore();
        Assertions.assertThat(store.getFirstPosition()).isEqualTo(MessageStoreTest.NO_MESSAGE_ID);
        final Message msg1 = new MessageBuilder("test").buildFinal();
        store.add(msg1);
        final Message msg2 = new MessageBuilder("test2").buildFinal();
        store.add(msg2);
        Assertions.assertThat(store.getFirstPosition()).isEqualTo(MessageStoreTest.MESSAGE_ID_1);
        Assertions.assertThat(store.getLatestPosition()).isEqualTo(MessageStoreTest.MESSAGE_ID_2);
        // send third, discard first two
        final Message msg3 = new MessageBuilder("test3").buildFinal();
        store.add(msg3);
        Assertions.assertThat(store.discardBefore(MessageStoreTest.MESSAGE_ID_3)).isEqualTo(2);
        Assertions.assertThat(store.getFirstPosition()).isEqualTo(MessageStoreTest.MESSAGE_ID_3);
        Assertions.assertThat(store.getLatestPosition()).isEqualTo(MessageStoreTest.MESSAGE_ID_3);
        final List<Message> postDiscard = store.getFrom(MessageStoreTest.MESSAGE_ID_3);
        Assertions.assertThat(postDiscard.size()).isEqualTo(1);
        Assertions.assertThat(postDiscard.get(0)).isSameAs(msg3);
    }

    @Test
    public void testManualDiscardingOutsideLimits() {
        // unlimited capacity
        final MessageStore store = new MessageStore();
        Assertions.assertThat(store.getFirstPosition()).isEqualTo(MessageStoreTest.NO_MESSAGE_ID);
        // ensure proper behavior on empty store
        Assertions.assertThat(store.discardBefore(0)).isEqualTo(0);
        Assertions.assertThat(store.discardBefore(MessageStoreTest.MESSAGE_ID_1)).isEqualTo(0);
        final Message msg1 = new MessageBuilder("test").buildFinal();
        store.add(msg1);
        final Message msg2 = new MessageBuilder("test2").buildFinal();
        store.add(msg2);
        // ensure proper behavior when out of bounds
        Assertions.assertThat(store.discardBefore(0)).isEqualTo(0);
        Assertions.assertThat(store.getAll().size()).isEqualTo(2);
        Assertions.assertThat(store.discardBefore(-1)).isEqualTo(0);
        Assertions.assertThat(store.getAll().size()).isEqualTo(2);
        Assertions.assertThat(store.discardBefore(MessageStoreTest.MESSAGE_ID_3)).isEqualTo(2);
        Assertions.assertThat(store.getAll().size()).isEqualTo(0);
        Assertions.assertThat(store.discardBefore(0)).isEqualTo(0);
        Assertions.assertThat(store.getAll().size()).isEqualTo(0);
        Assertions.assertThat(store.discardBefore(-1)).isEqualTo(0);
        Assertions.assertThat(store.getAll().size()).isEqualTo(0);
        Assertions.assertThat(store.discardBefore(1000)).isEqualTo(0);
        Assertions.assertThat(store.getAll().size()).isEqualTo(0);
        // and ensure it still works after we add another
        final Message msg3 = new MessageBuilder("test3").buildFinal();
        store.add(msg3);
        Assertions.assertThat(store.getAll().size()).isEqualTo(1);
        Assertions.assertThat(store.discardBefore(0)).isEqualTo(0);
        Assertions.assertThat(store.getAll().size()).isEqualTo(1);
        Assertions.assertThat(store.discardBefore(-1)).isEqualTo(0);
        Assertions.assertThat(store.getAll().size()).isEqualTo(1);
        Assertions.assertThat(store.discardBefore(1000)).isEqualTo(1);
        Assertions.assertThat(store.getAll().size()).isEqualTo(0);
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
    public void testValidRange() {
        final MessageStore store = new MessageStore();
        // add first message
        final Message msg = new MessageBuilder("test").buildFinal();
        store.add(msg);
        List<Message> range = store.getFromRange(0, 1);
        Assertions.assertThat(range.size()).isEqualTo(1);
        Assertions.assertThat(range.get(0)).isSameAs(msg);
        // add second message
        final Message msg2 = new MessageBuilder("test2").buildFinal();
        store.add(msg2);
        range = store.getFromRange(1, 2);
        Assertions.assertThat(range.size()).isEqualTo(1);
        Assertions.assertThat(range.get(0)).isSameAs(msg2);
    }
}
