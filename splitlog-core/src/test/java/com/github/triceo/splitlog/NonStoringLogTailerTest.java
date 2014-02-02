package com.github.triceo.splitlog;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class NonStoringLogTailerTest {

    private static final File watchedFile = new File("target/", "testing.log");
    private static LogWatch logwatch;
    private static LogWriter writer;

    @BeforeClass
    public static void startTailing() {
        NonStoringLogTailerTest.logwatch = LogWatchFactory.newLogWatch(NonStoringLogTailerTest.watchedFile);
        NonStoringLogTailerTest.writer = new LogWriter(NonStoringLogTailerTest.watchedFile);
    }

    @AfterClass
    public static void terminateTailing() {
        NonStoringLogTailerTest.writer.destroy();
        NonStoringLogTailerTest.logwatch.terminateTailing();
    }

    @Test
    public void testGetMessages() {
        final String message1 = "test";
        final String message2part1 = "test7";
        final String message2part2 = "test";
        final String message3part1 = "testXXX";
        final String message3part2 = "AAAtest";
        final AbstractLogTailer tailer = NonStoringLogTailerTest.logwatch.startTailing();
        // test simple messages
        NonStoringLogTailerTest.writer.write(message1, tailer);
        Assert.assertEquals(message1, tailer.getMessages().get(0).getRawMessage().getFirstLine());
        NonStoringLogTailerTest.writer.write(message1, tailer);
        Assert.assertEquals(message1, tailer.getMessages().get(1).getRawMessage().getFirstLine());
        // test multi-line messages; each line should be its own message
        NonStoringLogTailerTest.writer.write(message2part1 + "\n" + message2part2, tailer);
        NonStoringLogTailerTest.writer.write(message3part1 + "\r\n" + message3part2, tailer);
        Assert.assertEquals(message2part1, tailer.getMessages().get(2).getRawMessage().getFirstLine());
        Assert.assertEquals(message2part2, tailer.getMessages().get(3).getRawMessage().getFirstLine());
        Assert.assertEquals(message3part1, tailer.getMessages().get(4).getRawMessage().getFirstLine());
        Assert.assertEquals(message3part2, tailer.getMessages().get(5).getRawMessage().getFirstLine());
        NonStoringLogTailerTest.logwatch.terminateTailing(tailer);
    }

    @Test
    public void testTag() {
        final String message1 = "test";
        final String message2 = "test7";
        final String message3 = "test11";
        final String tag0 = "tag1";
        final String tag1 = "tag1";
        final String tag2 = "tag2";
        final String tag3 = "tag3";
        final AbstractLogTailer tailer = NonStoringLogTailerTest.logwatch.startTailing();
        tailer.tag(tag0);
        NonStoringLogTailerTest.writer.write(message1, tailer);
        tailer.tag(tag1);
        NonStoringLogTailerTest.writer.write(message2, tailer);
        tailer.tag(tag2);
        NonStoringLogTailerTest.writer.write(message3, tailer);
        tailer.tag(tag3);
        Assert.assertEquals(tag0, tailer.getMessages().get(0).getRawMessage().getFirstLine());
        Assert.assertEquals(message1, tailer.getMessages().get(1).getRawMessage().getFirstLine());
        Assert.assertEquals(tag1, tailer.getMessages().get(2).getRawMessage().getFirstLine());
        Assert.assertEquals(message2, tailer.getMessages().get(3).getRawMessage().getFirstLine());
        Assert.assertEquals(tag2, tailer.getMessages().get(4).getRawMessage().getFirstLine());
        Assert.assertEquals(message3, tailer.getMessages().get(5).getRawMessage().getFirstLine());
        Assert.assertEquals(tag3, tailer.getMessages().get(6).getRawMessage().getFirstLine());
        NonStoringLogTailerTest.logwatch.terminateTailing(tailer);
    }

}
