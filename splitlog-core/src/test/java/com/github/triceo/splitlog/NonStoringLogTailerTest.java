package com.github.triceo.splitlog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.triceo.splitlog.conditions.LineCondition;

@RunWith(Parameterized.class)
public class NonStoringLogTailerTest {

    private static File getTempFile() {
        try {
            return File.createTempFile("splitlog-", ".log");
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temp files.", e);
        }
    }

    // will verify various configs of log watch
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { LogWatchBuilder.forFile(NonStoringLogTailerTest.getTempFile()) },
                { LogWatchBuilder.forFile(NonStoringLogTailerTest.getTempFile()).closingAfterReading()
                        .ignoringPreexistingContent() },
                { LogWatchBuilder.forFile(NonStoringLogTailerTest.getTempFile()).closingAfterReading() },
                { LogWatchBuilder.forFile(NonStoringLogTailerTest.getTempFile()).ignoringPreexistingContent() } });
    }

    private final LogWatchBuilder builder;
    private LogWatch logwatch;
    private LogWriter writer;

    public NonStoringLogTailerTest(final LogWatchBuilder builder) {
        this.builder = builder;
    }

    @Before
    public void startEverything() {
        // prepare file
        final File toWrite = this.builder.getFileToWatch();
        if (toWrite.exists()) {
            toWrite.delete();
        }
        try {
            toWrite.createNewFile();
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create temp files.", e);
        }
        // prepare writer
        this.writer = new LogWriter(toWrite);
        // and start the log watch
        this.logwatch = this.builder.build();
    }

    @After
    public void destroyEverything() {
        this.writer.destroy();
        if (!this.logwatch.isTerminated()) {
            this.logwatch.terminateTailing();
        }
    }

    @Test
    public void testGetMessages() {
        final String message1 = "test";
        final String message2part1 = "test1";
        final String message2part2 = "test2";
        final String message3part1 = "test3";
        final String message3part2 = "test4";
        final LogTailer tailer = this.logwatch.startTailing();
        // test simple messages
        String result = this.writer.write(message1, tailer);
        Assert.assertEquals(message1, result);
        result = this.writer.write(message1, tailer);
        Assert.assertEquals(message1, result);
        result = this.writer.write(message2part1 + "\n" + message2part2, tailer);
        Assert.assertEquals(message2part2, result);
        result = this.writer.write(message3part1 + "\r\n" + message3part2, tailer);
        Assert.assertEquals(message3part2, result);
        // now validate the results
        final List<Message> messages = tailer.getMessages();
        Assert.assertEquals(5, messages.size());
        Assert.assertEquals(message1, messages.get(1).getLines().get(0));
        Assert.assertEquals(message2part1, messages.get(2).getLines().get(0));
        Assert.assertEquals(message2part2, messages.get(3).getLines().get(0));
        Assert.assertEquals(message3part1, messages.get(4).getLines().get(0));
        // final part of the message, message3part2, will remain unflushed
        this.logwatch.terminateTailing(tailer);
        Assert.assertTrue(tailer.isTerminated());
    }

    @Test
    public void testTag() {
        final String message1 = "test";
        final String message2 = "test7";
        final String message3 = "test11";
        final String tag0 = "tag1";
        final String tag1 = "tag1";
        final String tag2 = "tag2";
        final LogTailer tailer = this.logwatch.startTailing();
        tailer.tag(tag0);
        String result = this.writer.write(message1, tailer);
        Assert.assertEquals(message1, result);
        result = this.writer.write(message2, tailer);
        Assert.assertEquals(message2, result);
        tailer.tag(tag1); // message1 will only be written after message2 has
                          // been sent
        result = this.writer.write(message3, tailer);
        Assert.assertEquals(message3, result);
        tailer.tag(tag2); // message2 will only be written after message3 has
                          // been sent
        final List<Message> messages = tailer.getMessages();
        Assert.assertEquals(5, messages.size());
        Assert.assertEquals(tag0, messages.get(0).getLines().get(0));
        Assert.assertEquals(message1, messages.get(1).getLines().get(0));
        Assert.assertEquals(tag1, messages.get(2).getLines().get(0));
        Assert.assertEquals(message2, messages.get(3).getLines().get(0));
        Assert.assertEquals(tag2, messages.get(4).getLines().get(0));
    }

    @Test
    public void testConcurrency() {
        final String message1 = "test";
        final LogTailer tailer = this.logwatch.startTailing();
        // make sure the messages are received
        String result = this.writer.write(message1, tailer);
        result = this.writer.write(message1, tailer);
        result = this.writer.write(message1, tailer);
        Assert.assertEquals(message1, result);
        // now get a list of messages
        final List<Message> messages = tailer.getMessages();
        // write additional message
        result = this.writer.write(message1, tailer);
        result = this.writer.write(message1, tailer);
        Assert.assertEquals(message1, result);
        // and iterate through the original list to check for CMEs
        Assert.assertEquals(2, messages.size());
        for (final Message msg : messages) {
            Assert.assertEquals(message1, msg.getLines().get(0));
        }
        // and output the new list
        try {
            tailer.write(new FileOutputStream(new File("target/", "testConcurrency.log")));
        } catch (final FileNotFoundException e) {
            // do nothing
        }
    }

    @Test
    public void testNesting() {
        final String message1 = "test1";
        final String message2 = "test2";
        final String message3 = "test3";
        final String message4 = "test4";
        final String message5 = "test5";
        final LogTailer tailer = this.logwatch.startTailing();
        // make sure the messages are received by the first tailer
        this.writer.write(message1, tailer);
        String result = this.writer.write(message2, tailer);
        Assert.assertEquals(message2, result);
        Assert.assertEquals(1, tailer.getMessages().size());
        // start a second tailer, send some messages
        final LogTailer nestedTailer = this.logwatch.startTailing();
        result = this.writer.write(message3, tailer);
        result = this.writer.write(message4, tailer);
        Assert.assertEquals(message4, result);
        this.logwatch.terminateTailing(nestedTailer);
        // send another message, so the original tailer has something extra
        Assert.assertEquals(true, nestedTailer.isTerminated());
        result = this.writer.write(message5, tailer);
        // and make sure that the original tailer has all messages
        Assert.assertEquals(4, tailer.getMessages().size());
        Assert.assertEquals(message1, tailer.getMessages().get(0).getLines().get(0));
        Assert.assertEquals(message2, tailer.getMessages().get(1).getLines().get(0));
        Assert.assertEquals(message3, tailer.getMessages().get(2).getLines().get(0));
        Assert.assertEquals(message4, tailer.getMessages().get(3).getLines().get(0));
        // and the nested tailer has only the two while it was running
        Assert.assertEquals(2, nestedTailer.getMessages().size());
        Assert.assertEquals(message2, nestedTailer.getMessages().get(0).getLines().get(0));
        Assert.assertEquals(message3, nestedTailer.getMessages().get(1).getLines().get(0));
    }

    @Test
    public void testTermination() {
        Assert.assertFalse("Log watch terminated immediately after starting.", this.logwatch.isTerminated());
        final LogTailer tailer1 = this.logwatch.startTailing();
        Assert.assertFalse("Tailer terminated immediately after starting.", this.logwatch.isTerminated(tailer1));
        final LogTailer tailer2 = this.logwatch.startTailing();
        Assert.assertTrue("Wrong termination result.", this.logwatch.terminateTailing(tailer1));
        Assert.assertFalse("Wrong termination result.", this.logwatch.terminateTailing(tailer1));
        Assert.assertFalse("Tailer terminated without termination.", this.logwatch.isTerminated(tailer2));
        Assert.assertTrue("Tailer not terminated after termination.", this.logwatch.isTerminated(tailer1));
        Assert.assertTrue("Wrong termination result.", this.logwatch.terminateTailing());
        Assert.assertFalse("Wrong termination result.", this.logwatch.terminateTailing());
        Assert.assertTrue("Tailer not terminated after termination.", this.logwatch.isTerminated(tailer2));
        Assert.assertTrue("Log watch not terminated after termination.", this.logwatch.isTerminated());
    }

    // FIXME we should also test waitFor() on a MessageCondition
    @Test
    public void testWaitForAfterPreviousFailed() {
        final LogTailer tailer = this.logwatch.startTailing();
        // this call will fail, since we're not writing anything
        tailer.waitFor(new LineCondition() {

            @Override
            public boolean accept(final String evaluate) {
                return true;
            }

        }, 1, TimeUnit.SECONDS);
        // these calls should succeed
        final String message = "test";
        String result = this.writer.write(message, tailer);
        Assert.assertEquals(message, result);
        result = this.writer.write(message, tailer);
        Assert.assertEquals(message, result);
        final List<Message> messages = tailer.getMessages();
        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(message, messages.get(0).getLines().get(0));
        this.logwatch.terminateTailing(tailer);
    }

}
