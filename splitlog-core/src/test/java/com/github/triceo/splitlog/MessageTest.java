package com.github.triceo.splitlog;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class MessageTest {

    private static final MessageClassifier<MessageType> TYPE_CLASSIFIER = new MessageClassifier<MessageType>() {

        public MessageType classify(final RawMessage m) {
            return MessageType.LOG;
        }

    };

    private static final MessageClassifier<MessageSeverity> SEVERITY_CLASSIFIER = new MessageClassifier<MessageSeverity>() {

        public MessageSeverity classify(final RawMessage m) {
            return MessageSeverity.INFO;
        }

    };

    @Test
    public void testEqualsMessage() {
        final String[] lines = new String[] { "Test", "Test2", "Test3" };
        final RawMessage raw1 = new RawMessage(Arrays.asList(lines));
        final RawMessage raw2 = new RawMessage(Arrays.asList(lines));
        final RawMessage raw3 = new RawMessage(Arrays.asList(lines).subList(1, 3));
        Assert.assertEquals(new Message(raw1), new Message(raw1));
        Assert.assertEquals(new Message(raw1), new Message(raw2));
        Assert.assertNotEquals(new Message(raw1), new Message(raw3));
    }

    @Test
    public void testEqualsTag() {
        final String line = "Test";
        final Message msg1 = new Message(line);
        final Message msg2 = new Message(line);
        final Message msg3 = new Message(line + "2");
        Assert.assertEquals(msg1, msg1);
        Assert.assertEquals(msg1, msg2);
        Assert.assertNotEquals(msg1, msg3);
    }

    @Test
    public void testGetSeverity() {
        final String[] lines = new String[] { "Test", "Test2", "Test3" };
        final RawMessage raw = new RawMessage(Arrays.asList(lines));
        final Message msg = new Message(raw, null, MessageTest.SEVERITY_CLASSIFIER);
        Assert.assertEquals(MessageSeverity.INFO, msg.getSeverity());
    }

    @Test
    public void testGetType() {
        final String[] lines = new String[] { "Test", "Test2", "Test3" };
        final RawMessage raw = new RawMessage(Arrays.asList(lines));
        final Message msg = new Message(raw, MessageTest.TYPE_CLASSIFIER);
        Assert.assertEquals(MessageType.LOG, msg.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMessage() {
        final RawMessage raw = null;
        new Message(raw);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTag() {
        final String raw = null;
        new Message(raw);
    }

    @Test
    public void testTag() {
        final String line = "Test";
        final Message msg = new Message(line);
        Assert.assertEquals(MessageSeverity.UNKNOWN, msg.getSeverity());
        Assert.assertEquals(MessageType.TAG, msg.getType());
        Assert.assertEquals(1, msg.getRawMessage().getLines().size());
        Assert.assertEquals(line, msg.getRawMessage().getFirstLine());
    }

}
