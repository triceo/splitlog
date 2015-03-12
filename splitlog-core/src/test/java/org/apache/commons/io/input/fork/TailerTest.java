/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * TODO remove once https://issues.apache.org/jira/browse/IO-444 is fixed
 */
package org.apache.commons.io.input.fork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.fork.Tailer;
import org.apache.commons.io.input.fork.TailerListener;
import org.apache.commons.io.testtools.FileBasedTestCase;

/**
 * Tests for {@link Tailer}.
 *
 * @version $Id$
 */
public class TailerTest extends FileBasedTestCase {

    /**
     * Test {@link TailerListener} implementation.
     */
    private static class TestTailerListener implements TailerListener {

        volatile Exception exception = null;

        volatile int initialised = 0;

        // Must be synchronised because it is written by one thread and read by another
        private final List<String> lines = Collections.synchronizedList(new ArrayList<String>());

        volatile int notFound = 0;

        volatile int rotated = 0;

        @Override
        public void begin() {
            // do nothing
        }

        public void clear() {
            this.lines.clear();
        }

        @Override
        public void commit() {
            // do nothing
        }

        @Override
        public void destroy() {
            // do nothing
        }

        @Override
        public void fileNotFound() {
            this.notFound++; // not atomic, but OK because only updated here.
        }

        @Override
        public void fileRotated() {
            this.rotated++; // not atomic, but OK because only updated here.
        }

        public List<String> getLines() {
            return this.lines;
        }

        @Override
        public void handle(final Exception e) {
            this.exception = e;
        }

        @Override
        public void handle(final String line) {
            this.lines.add(line);
        }

        @Override
        public void init(final Tailer tailer) {
            this.initialised++; // not atomic, but OK because only updated here.
        }
    }

    private Tailer tailer;

    public TailerTest(final String name) {
        super(name);
    }

    @Override
    protected void createFile(final File file, final long size)
            throws IOException {
        super.createFile(file, size);

        // try to make sure file is found
        // (to stop continuum occasionally failing)
        RandomAccessFile reader = null;
        try {
            while (reader == null) {
                try {
                    reader = new RandomAccessFile(file.getPath(), "r");
                } catch (final FileNotFoundException e) {
                }
                try {
                    Thread.sleep(200L);
                } catch (final InterruptedException e) {
                    // ignore
                }
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (this.tailer != null) {
            this.tailer.stop();
            Thread.sleep(1000);
        }
        FileUtils.deleteDirectory(FileBasedTestCase.getTestDirectory());
        Thread.sleep(1000);
    }

    public void testBufferBreak() throws Exception {
        final long delay = 50;

        final File file = new File(FileBasedTestCase.getTestDirectory(), "testBufferBreak.txt");
        this.createFile(file, 0);
        this.writeString(file, "SBTOURIST\n");

        final TestTailerListener listener = new TestTailerListener();
        this.tailer = new Tailer(file, listener, delay, false, 1);

        final Thread thread = new Thread(this.tailer);
        thread.start();

        List<String> lines = listener.getLines();
        while (lines.isEmpty() || !lines.get(lines.size() - 1).equals("SBTOURIST")) {
            lines = listener.getLines();
        }

        listener.clear();
    }

    /**
     * Tests [IO-357][Tailer] InterruptedException while the thead is sleeping is silently ignored.
     *
     * @throws Exception
     */
    public void testInterrupt() throws Exception {
        final File file = new File(FileBasedTestCase.getTestDirectory(), "nosuchfile");
        TestCase.assertFalse("nosuchfile should not exist", file.exists());
        final TestTailerListener listener = new TestTailerListener();
        // Use a long delay to try to make sure the test thread calls interrupt() while the tailer thread is sleeping.
        final int delay = 1000;
        final int idle = 50; // allow time for thread to work
        Tailer tailer = new Tailer(file, listener, delay, false, 4096);
        final Thread thread = new Thread(tailer);
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(idle);
        thread.interrupt();
        tailer = null;
        Thread.sleep(delay + idle);
        TestCase.assertNotNull("Missing InterruptedException", listener.exception);
        TestCase.assertTrue("Unexpected Exception: " + listener.exception, listener.exception instanceof InterruptedException);
        TestCase.assertEquals("Expected init to be called", 1, listener.initialised);
        TestCase.assertTrue("fileNotFound should be called", listener.notFound > 0);
        TestCase.assertEquals("fileRotated should be not be called", 0, listener.rotated);
    }

    public void testIO335() throws Exception { // test CR behaviour
        // Create & start the Tailer
        final long delayMillis = 50;
        final File file = new File(FileBasedTestCase.getTestDirectory(), "tailer-testio334.txt");
        this.createFile(file, 0);
        final TestTailerListener listener = new TestTailerListener();
        this.tailer = new Tailer(file, listener, delayMillis, false);
        final Thread thread = new Thread(this.tailer);
        thread.start();

        // Write some lines to the file
        this.writeString(file, "CRLF\r\n", "LF\n", "CR\r", "CRCR\r\r", "trail");
        final long testDelayMillis = delayMillis * 10;
        Thread.sleep(testDelayMillis);
        final List<String> lines = listener.getLines();
        TestCase.assertEquals("line count", 4, lines.size());
        TestCase.assertEquals("line 1", "CRLF", lines.get(0));
        TestCase.assertEquals("line 2", "LF", lines.get(1));
        TestCase.assertEquals("line 3", "CR", lines.get(2));
        TestCase.assertEquals("line 4", "CRCR\r", lines.get(3));

        // Stop
        this.tailer.stop();
        this.tailer=null;
        thread.interrupt();
        Thread.sleep(testDelayMillis);
    }

    public void testLongFile() throws Exception {
        final long delay = 50;

        final File file = new File(FileBasedTestCase.getTestDirectory(), "testLongFile.txt");
        this.createFile(file, 0);
        final Writer writer = new FileWriter(file, true);
        for (int i = 0; i < 100000; i++) {
            writer.write("LineLineLineLineLineLineLineLineLineLine\n");
        }
        writer.write("SBTOURIST\n");
        IOUtils.closeQuietly(writer);

        final TestTailerListener listener = new TestTailerListener();
        this.tailer = new Tailer(file, listener, delay, false);

        final long start = System.currentTimeMillis();

        final Thread thread = new Thread(this.tailer);
        thread.start();

        List<String> lines = listener.getLines();
        while (lines.isEmpty() || !lines.get(lines.size() - 1).equals("SBTOURIST")) {
            lines = listener.getLines();
        }
        System.out.println("Elapsed: " + (System.currentTimeMillis() - start));

        listener.clear();
    }

    public void testMultiByteBreak() throws Exception {
        System.out.println("testMultiByteBreak() Default charset: "+Charset.defaultCharset().displayName());
        final long delay = 50;
        final File origin = new File(this.getClass().getResource("/test-file-utf8.bin").toURI());
        final File file = new File(FileBasedTestCase.getTestDirectory(), "testMultiByteBreak.txt");
        this.createFile(file, 0);
        final TestTailerListener listener = new TestTailerListener();
        final String osname = System.getProperty("os.name");
        final boolean isWindows = osname.startsWith("Windows");
        // Need to use UTF-8 to read & write the file otherwise it can be corrupted (depending on the default charset)
        final Charset charsetUTF8 = Charsets.UTF_8;
        this.tailer = new Tailer(file, charsetUTF8, listener, delay, false, isWindows, 4096);
        final Thread thread = new Thread(this.tailer);
        thread.start();

        final Writer out = new OutputStreamWriter(new FileOutputStream(file), charsetUTF8);
        BufferedReader reader = null;
        try{
            final List<String> lines = new ArrayList<String>();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(origin), charsetUTF8));
            String line = null;
            while((line = reader.readLine()) != null){
                out.write(line);
                out.write("\n");
                lines.add(line);
            }
            out.close(); // ensure data is written

            final long testDelayMillis = delay * 10;
            Thread.sleep(testDelayMillis);
            final List<String> tailerlines = listener.getLines();
            TestCase.assertEquals("line count",lines.size(),tailerlines.size());
            for(int i = 0,len = lines.size();i<len;i++){
                final String expected = lines.get(i);
                final String actual = tailerlines.get(i);
                if (!expected.equals(actual)) {
                    TestCase.fail("Line: " + i
                            + "\nExp: (" + expected.length() + ") " + expected
                            + "\nAct: (" + actual.length() + ") "+ actual);
                }
            }
        }finally{
            this.tailer.stop();
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(out);
        }
    }

    public void testStopWithNoFile() throws Exception {
        final File file = new File(FileBasedTestCase.getTestDirectory(),"nosuchfile");
        TestCase.assertFalse("nosuchfile should not exist", file.exists());
        final TestTailerListener listener = new TestTailerListener();
        final int delay = 100;
        final int idle = 50; // allow time for thread to work
        this.tailer = Tailer.create(file, listener, delay, false);
        Thread.sleep(idle);
        this.tailer.stop();
        this.tailer=null;
        Thread.sleep(delay+idle);
        TestCase.assertNull("Should not generate Exception", listener.exception);
        TestCase.assertEquals("Expected init to be called", 1 , listener.initialised);
        TestCase.assertTrue("fileNotFound should be called", listener.notFound > 0);
        TestCase.assertEquals("fileRotated should be not be called", 0 , listener.rotated);
    }

    public void testStopWithNoFileUsingExecutor() throws Exception {
        final File file = new File(FileBasedTestCase.getTestDirectory(),"nosuchfile");
        TestCase.assertFalse("nosuchfile should not exist", file.exists());
        final TestTailerListener listener = new TestTailerListener();
        final int delay = 100;
        final int idle = 50; // allow time for thread to work
        this.tailer = new Tailer(file, listener, delay, false);
        final Executor exec = new ScheduledThreadPoolExecutor(1);
        exec.execute(this.tailer);
        Thread.sleep(idle);
        this.tailer.stop();
        this.tailer=null;
        Thread.sleep(delay+idle);
        TestCase.assertNull("Should not generate Exception", listener.exception);
        TestCase.assertEquals("Expected init to be called", 1 , listener.initialised);
        TestCase.assertTrue("fileNotFound should be called", listener.notFound > 0);
        TestCase.assertEquals("fileRotated should be not be called", 0 , listener.rotated);
    }

    public void testTailer() throws Exception {

        // Create & start the Tailer
        final long delayMillis = 50;
        final File file = new File(FileBasedTestCase.getTestDirectory(), "tailer1-test.txt");
        this.createFile(file, 0);
        final TestTailerListener listener = new TestTailerListener();
        final String osname = System.getProperty("os.name");
        final boolean isWindows = osname.startsWith("Windows");
        this.tailer = new Tailer(file, listener, delayMillis, false, isWindows);
        final Thread thread = new Thread(this.tailer);
        thread.start();

        // Write some lines to the file
        this.write(file, "Line one", "Line two");
        final long testDelayMillis = delayMillis * 10;
        Thread.sleep(testDelayMillis);
        List<String> lines = listener.getLines();
        TestCase.assertEquals("1 line count", 2, lines.size());
        TestCase.assertEquals("1 line 1", "Line one", lines.get(0));
        TestCase.assertEquals("1 line 2", "Line two", lines.get(1));
        listener.clear();

        // Write another line to the file
        this.write(file, "Line three");
        Thread.sleep(testDelayMillis);
        lines = listener.getLines();
        TestCase.assertEquals("2 line count", 1, lines.size());
        TestCase.assertEquals("2 line 3", "Line three", lines.get(0));
        listener.clear();

        // Check file does actually have all the lines
        lines = FileUtils.readLines(file, "UTF-8");
        TestCase.assertEquals("3 line count", 3, lines.size());
        TestCase.assertEquals("3 line 1", "Line one", lines.get(0));
        TestCase.assertEquals("3 line 2", "Line two", lines.get(1));
        TestCase.assertEquals("3 line 3", "Line three", lines.get(2));

        // Delete & re-create
        file.delete();
        final boolean exists = file.exists();
        TestCase.assertFalse("File should not exist", exists);
        this.createFile(file, 0);
        Thread.sleep(testDelayMillis);

        // Write another line
        this.write(file, "Line four");
        Thread.sleep(testDelayMillis);
        lines = listener.getLines();
        TestCase.assertEquals("4 line count", 1, lines.size());
        TestCase.assertEquals("4 line 3", "Line four", lines.get(0));
        listener.clear();

        // Stop
        this.tailer.stop();
        this.tailer=null;
        thread.interrupt();
        Thread.sleep(testDelayMillis * 4);
        this.write(file, "Line five");
        TestCase.assertEquals("4 line count", 0, listener.getLines().size());
        TestCase.assertNotNull("Missing InterruptedException", listener.exception);
        TestCase.assertTrue("Unexpected Exception: " + listener.exception, listener.exception instanceof InterruptedException);
        TestCase.assertEquals("Expected init to be called", 1 , listener.initialised);
        TestCase.assertEquals("fileNotFound should not be called", 0 , listener.notFound);
        TestCase.assertEquals("fileRotated should be be called", 1 , listener.rotated);
    }

    public void testTailerEof() throws Exception {
        // Create & start the Tailer
        final long delay = 50;
        final File file = new File(FileBasedTestCase.getTestDirectory(), "tailer2-test.txt");
        this.createFile(file, 0);
        final TestTailerListener listener = new TestTailerListener();
        final Tailer tailer = new Tailer(file, listener, delay, false);
        final Thread thread = new Thread(tailer);
        thread.start();

        // Write some lines to the file
        final FileWriter writer = null;
        try {
            this.writeString(file, "Line");

            Thread.sleep(delay * 2);
            List<String> lines = listener.getLines();
            TestCase.assertEquals("1 line count", 0, lines.size());

            this.writeString(file, " one\n");
            Thread.sleep(delay * 2);
            lines = listener.getLines();

            TestCase.assertEquals("1 line count", 1, lines.size());
            TestCase.assertEquals("1 line 1", "Line one", lines.get(0));

            listener.clear();
        } finally {
            tailer.stop();
            Thread.sleep(delay * 2);
            IOUtils.closeQuietly(writer);
        }
    }

    /** Append some lines to a file */
    private void write(final File file, final String... lines) throws Exception {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            for (final String line : lines) {
                writer.write(line + "\n");
            }
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /** Append a string to a file */
    private void writeString(final File file, final String ... strings) throws Exception {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            for (final String string : strings) {
                writer.write(string);
            }
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
