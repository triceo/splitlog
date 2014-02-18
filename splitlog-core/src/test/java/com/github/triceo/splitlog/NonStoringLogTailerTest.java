package com.github.triceo.splitlog;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NonStoringLogTailerTest {

    private static final File watchedFile = new File("target/", "testing.log");

    // will verify various configs of log watch
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { LogWatch.forFile(NonStoringLogTailerTest.watchedFile) },
                { LogWatch.forFile(NonStoringLogTailerTest.watchedFile).closingAfterReading()
                        .ignoringPreexistingContent() },
                { LogWatch.forFile(NonStoringLogTailerTest.watchedFile).closingAfterReading() },
                { LogWatch.forFile(NonStoringLogTailerTest.watchedFile).ignoringPreexistingContent() } });
    }

    private final LogWatchBuilder builder;
    private final LogWriter writer = new LogWriter(NonStoringLogTailerTest.watchedFile);

    public NonStoringLogTailerTest(final LogWatchBuilder builder) {
        this.builder = builder;
    }

    @Before
    public void deleteLogFile() {
        if (NonStoringLogTailerTest.watchedFile.exists()) {
            NonStoringLogTailerTest.watchedFile.delete();
        }
    }

    @After
    public void destroyWriter() {
        this.writer.destroy();
    }

    @Test
    public void testGetMessages() {
        final String message1 = "test";
        final String message2part1 = "test1";
        final String message2part2 = "test2";
        final String message3part1 = "test3";
        final String message3part2 = "test4";
        final LogWatch logwatch = this.builder.build();
        final AbstractLogTailer tailer = logwatch.startTailing();
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
        final LogWatch logwatch = this.builder.build();
        final AbstractLogTailer tailer = logwatch.startTailing();
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
        Assert.assertEquals(tag0, tailer.getMessages().get(0).getLines().get(0));
        Assert.assertEquals(message1, tailer.getMessages().get(1).getLines().get(0));
        Assert.assertEquals(tag1, tailer.getMessages().get(2).getLines().get(0));
        Assert.assertEquals(message2, tailer.getMessages().get(3).getLines().get(0));
        Assert.assertEquals(tag2, tailer.getMessages().get(4).getLines().get(0));
        logwatch.terminateTailing();
    }

}
