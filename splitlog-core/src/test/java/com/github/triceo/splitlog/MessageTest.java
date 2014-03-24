package com.github.triceo.splitlog;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MessageTest {

    private Message buildMessage(final Collection<String> raw) {
        final List<String> lines = new LinkedList<String>(raw);
        return new MessageBuilder(lines.get(0)).add(lines.subList(1, lines.size())).buildFinal();
    }

    @Test
    public void testEqualsMessage() {
        final String[] lines = new String[] { "Test", "Test2", "Test3" };
        final Collection<String> raw1 = Arrays.asList(lines);
        final Collection<String> raw2 = Arrays.asList(lines);
        final Collection<String> raw3 = Arrays.asList(lines).subList(1, 3);
        final Message message0 = this.buildMessage(raw1);
        Assert.assertEquals(message0, message0);
        final Message message1 = this.buildMessage(raw2);
        Assert.assertNotEquals(message0, message1);
        Assert.assertNotEquals(message1, this.buildMessage(raw2));
        Assert.assertNotEquals(message1, this.buildMessage(raw3));
    }

    @Test
    public void testEqualsTag() {
        final String line = "Test";
        final Message msg1 = new MessageBuilder(line).buildTag();
        final Message msg2 = new MessageBuilder(line).buildTag();
        final Message msg3 = new MessageBuilder(line + "2").buildTag();
        Assert.assertEquals(msg1, msg1);
        Assert.assertNotEquals(msg1, msg2);
        Assert.assertNotEquals(msg1, msg3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTag() {
        final String raw = null;
        new MessageBuilder(raw).buildTag();
    }

    @Test
    public void testTag() {
        final String line = "Test";
        final Message msg = new MessageBuilder(line).buildTag();
        Assert.assertEquals(MessageSeverity.UNKNOWN, msg.getSeverity());
        Assert.assertEquals(MessageType.TAG, msg.getType());
        Assert.assertEquals(1, msg.getLines().size());
        Assert.assertEquals(line, msg.getLines().get(0));
    }

}
