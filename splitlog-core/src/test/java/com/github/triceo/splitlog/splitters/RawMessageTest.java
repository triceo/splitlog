package com.github.triceo.splitlog.splitters;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.github.triceo.splitlog.splitters.RawMessage;

public class RawMessageTest {

    @Test
    public void testGetLines() {
        final String[] lines = new String[] { "Line1", "Line2", "Line2" };
        final RawMessage raw = new RawMessage(Arrays.asList(lines));
        Assert.assertEquals(lines.length, raw.getLines().size());
        Assert.assertEquals(lines[0], raw.getFirstLine());
        Assert.assertEquals(lines[0], raw.getLines().get(0));
        Assert.assertEquals(lines[1], raw.getLines().get(1));
        Assert.assertEquals(lines[2], raw.getLines().get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullLine() {
        final String[] lines = new String[] { "Line1", null, "Line2" };
        new RawMessage(Arrays.asList(lines));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRawMessageEmpty() {
        new RawMessage(Collections.<String> emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRawMessageNull() {
        new RawMessage(null);
    }

}
