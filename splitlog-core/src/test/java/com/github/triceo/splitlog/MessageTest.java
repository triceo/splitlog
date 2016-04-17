package com.github.triceo.splitlog;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageSeverity;
import com.github.triceo.splitlog.api.MessageType;

public class MessageTest extends AbstractSplitlogTest {

    private Message buildMessage(final Collection<String> raw) {
        final List<String> lines = new LinkedList<>(raw);
        return new MessageBuilder(lines.get(0)).add(lines.subList(1, lines.size())).buildFinal();
    }

    @Test
    public void testEqualsMessage() {
        final String[] lines = new String[] { "Test", "Test2", "Test3" };
        final Collection<String> raw1 = Arrays.asList(lines);
        final Collection<String> raw2 = Arrays.asList(lines);
        final Collection<String> raw3 = Arrays.asList(lines).subList(1, 3);
        final Message message0 = this.buildMessage(raw1);
        Assertions.assertThat(message0).isEqualTo(message0);
        final Message message1 = this.buildMessage(raw2);
        Assertions.assertThat(message1).isNotEqualTo(message0);
        Assertions.assertThat(this.buildMessage(raw2)).isNotEqualTo(message1);
        Assertions.assertThat(this.buildMessage(raw3)).isNotEqualTo(message1);
    }

    @Test
    public void testEqualsTag() {
        final String line = "Test";
        final Message msg1 = new MessageBuilder(line).buildTag();
        final Message msg2 = new MessageBuilder(line).buildTag();
        final Message msg3 = new MessageBuilder(line + "2").buildTag();
        Assertions.assertThat(msg1).isEqualTo(msg1);
        Assertions.assertThat(msg2).isNotEqualTo(msg1);
        Assertions.assertThat(msg3).isNotEqualTo(msg1);
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
        Assertions.assertThat(msg.getSeverity()).isEqualTo(MessageSeverity.UNKNOWN);
        Assertions.assertThat(msg.getType()).isEqualTo(MessageType.TAG);
        Assertions.assertThat(msg.getLines().size()).isEqualTo(1);
        Assertions.assertThat(msg.getLines().get(0)).isEqualTo(line);
    }

}
