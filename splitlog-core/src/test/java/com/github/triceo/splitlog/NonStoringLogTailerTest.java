package com.github.triceo.splitlog;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NonStoringLogTailerTest {

    private static final File watchedFile = new File("target/", "testing.log");
    private static LogWriter writer;

    @BeforeClass
    public static void startTailing() {
        NonStoringLogTailerTest.writer = new LogWriter(NonStoringLogTailerTest.watchedFile);
    }

    @AfterClass
    public static void terminateTailing() {
        NonStoringLogTailerTest.writer.destroy();
    }

    @Before
    public void deleteLogFile() {
        if (NonStoringLogTailerTest.watchedFile.exists()) {
            NonStoringLogTailerTest.watchedFile.delete();
        }
    }

    @Test
    public void testGetMessages() {
        final String message1 = "test";
        final String message2part1 = "test1";
        final String message2part2 = "test2";
        final String message3part1 = "test3";
        final String message3part2 = "test4";
        final LogWatch logwatch = LogWatchFactory.newLogWatch(NonStoringLogTailerTest.watchedFile);
        final AbstractLogTailer tailer = logwatch.startTailing();
        // test simple messages
        NonStoringLogTailerTest.writer.write(message1, tailer);
        NonStoringLogTailerTest.writer.writeWithWaiting(message1, tailer);
        // test multi-line messages; each line should be its own message
        NonStoringLogTailerTest.writer.writeWithWaiting(message2part1 + "\n" + message2part2, tailer);
        NonStoringLogTailerTest.writer.writeWithWaiting(message3part1 + "\r\n" + message3part2, tailer);
        try {
            /*
             * we're only waiting for one message, while there will be two. this
             * wait prevents CME in the next step.
             */
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            // nothing to do
        }
        final List<Message> messages = tailer.getMessages();
        Assert.assertEquals(message1, messages.get(0).getLines().get(0));
        Assert.assertEquals(message1, messages.get(1).getLines().get(0));
        Assert.assertEquals(message2part1, messages.get(2).getLines().get(0));
        Assert.assertEquals(message2part2, messages.get(3).getLines().get(0));
        Assert.assertEquals(message3part1, messages.get(4).getLines().get(0));
        // final part of the message, message3part2, will remain unflushed
        logwatch.terminateTailing(tailer);
        logwatch.terminateTailing();
    }

    @Test
    public void testTag() {
        final String message1 = "test";
        final String message2 = "test7";
        final String message3 = "test11";
        final String tag0 = "tag1";
        final String tag1 = "tag1";
        final String tag2 = "tag2";
        final LogWatch logwatch = LogWatchFactory.newLogWatch(NonStoringLogTailerTest.watchedFile);
        final AbstractLogTailer tailer = logwatch.startTailing();
        tailer.tag(tag0);
        NonStoringLogTailerTest.writer.write(message1, tailer);
        NonStoringLogTailerTest.writer.writeWithWaiting(message2, tailer);
        tailer.tag(tag1); // message1 will only be written after message2 has
                          // been sent
        NonStoringLogTailerTest.writer.writeWithWaiting(message3, tailer);
        tailer.tag(tag2); // message2 will only be written after message3 has
                          // been sent
        Assert.assertEquals(tag0, tailer.getMessages().get(0).getLines().get(0));
        Assert.assertEquals(message1, tailer.getMessages().get(1).getLines().get(0));
        Assert.assertEquals(tag1, tailer.getMessages().get(2).getLines().get(0));
        Assert.assertEquals(message2, tailer.getMessages().get(3).getLines().get(0));
        Assert.assertEquals(tag2, tailer.getMessages().get(4).getLines().get(0));
        logwatch.terminateTailing();
    }

}
