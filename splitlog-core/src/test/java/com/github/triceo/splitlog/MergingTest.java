package com.github.triceo.splitlog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.MergingFollower;
import com.github.triceo.splitlog.formatters.NoopMessageFormatter;

public class MergingTest extends DefaultFollowerBaseTest {

    private static final String MESSAGE_1 = "Message";
    private static final String MESSAGE_2 = "Second Message";
    private static final String MESSAGE_3 = "Third Message";
    private static final String MESSAGE_4 = "Fourth Message";
    private static final String MESSAGE_5 = "Fifth Message";

    @Test
    public void testMergeWithDifferentWatches() {
        // write into first file
        final LogWatch watch1 = LogWatchBuilder.forFile(DefaultFollowerBaseTest.getTempFile()).build();
        final Follower follower1 = watch1.startFollowing();
        final LogWriter writer1 = LogWriter.forFile(watch1.getWatchedFile());
        writer1.write(MergingTest.MESSAGE_1, follower1);
        // write into second file
        final LogWatch watch2 = LogWatchBuilder.forFile(DefaultFollowerBaseTest.getTempFile()).build();
        final Follower follower2 = watch2.startFollowing();
        final LogWriter writer2 = LogWriter.forFile(watch2.getWatchedFile());
        writer2.write(MergingTest.MESSAGE_2, follower2);
        // merge both
        final MergingFollower merge = follower2.mergeWith(follower1);
        Assertions.assertThat(merge.getMerged()).containsOnly(follower1, follower2);
        Assertions.assertThat(merge.getMessages()).hasSize(0); // zero ACCEPTED
        writer2.write(MergingTest.MESSAGE_3, follower2);
        Assertions.assertThat(merge.getMessages()).hasSize(1); // one ACCEPTED
        writer1.write(MergingTest.MESSAGE_4, follower1);
        // third follows the first file; merge should have the message just once
        final Follower follower3 = watch1.startFollowing();
        final MergingFollower merge2 = follower3.mergeWith(merge);
        Assertions.assertThat(merge.getMerged()).containsOnly(follower1, follower2);
        Assertions.assertThat(merge2).isNotSameAs(merge);
        Assertions.assertThat(merge2.getMerged()).containsOnly(follower1, follower2, follower3);
        writer1.write(MergingTest.MESSAGE_5, follower3);
        Assertions.assertThat(merge.getMessages()).hasSize(3);
        Assertions.assertThat(merge2.getMessages()).hasSize(3);
        // test writing
        final List<String> messages = new LinkedList<String>();
        messages.add(MergingTest.MESSAGE_1);
        messages.add(MergingTest.MESSAGE_2);
        messages.add(MergingTest.MESSAGE_4);
        try {
            final File f = DefaultFollowerBaseTest.getTempFile();
            merge2.write(new FileOutputStream(f), NoopMessageFormatter.INSTANCE);
            Assertions.assertThat(f).exists();
            final List<String> lines = FileUtils.readLines(f, "UTF-8");
            Assertions.assertThat(lines).isEqualTo(messages);
        } catch (final Exception e) {
            Assertions.fail("Couldn't write to file.");
        }
        // and now separate the second, making MESSAGE_2 disappear
        Assertions.assertThat(merge2.separate(follower2)).isTrue();
        Assertions.assertThat(merge2.getMessages()).hasSize(2);
        // now only MESSAGE_4 remains ACCEPTED
        Assertions.assertThat(merge2.separate(follower1)).isTrue();
        Assertions.assertThat(merge2.getMessages()).hasSize(1);
        // and now stop the followers to see what happens to the merges
        follower3.stop();
        Assertions.assertThat(follower3.isStopped()).isTrue();
        Assertions.assertThat(merge2.isStopped()).isTrue();
        Assertions.assertThat(merge.isStopped()).isFalse();
        // first watch is terminated, second one remains alive
        watch1.terminate();
        Assertions.assertThat(follower1.isStopped()).isTrue();
        Assertions.assertThat(merge2.isStopped()).isTrue();
        Assertions.assertThat(follower2.isStopped()).isFalse();
        Assertions.assertThat(merge.isStopped()).isFalse();
        merge.stop();
        Assertions.assertThat(merge.isStopped()).isTrue();
        Assertions.assertThat(merge2.isStopped()).isTrue();
        Assertions.assertThat(follower2.isStopped()).isTrue();
    }

    @Test
    public void testMergeWithSameLogWatch() {
        final Follower f = this.getLogWatch().startFollowing();
        final Follower f2 = this.getLogWatch().startFollowing();
        final MergingFollower mf = f.mergeWith(f2);
        // send the message; the merged should still contain only the one
        this.getWriter().write("test", f);
        this.getWriter().write("test2", f2);
        Assertions.assertThat(mf.getMessages().size()).isEqualTo(1);
        // add third follower, will only receive the second message
        final Follower f3 = this.getLogWatch().startFollowing();
        this.getWriter().write("test3", f3);
        final MergingFollower mf2 = mf.mergeWith(f3);
        Assertions.assertThat(mf2).isNotEqualTo(mf);
        Assertions.assertThat(mf.getMessages().size()).isEqualTo(2);
        Assertions.assertThat(mf2.getMessages().size()).isEqualTo(2);
        // remove both followers from first merge, verify results
        mf.separate(f);
        Assertions.assertThat(mf.getMessages().size()).isEqualTo(2);
        Assertions.assertThat(mf.isStopped()).isFalse();
        Assertions.assertThat(this.getLogWatch().stopFollowing(f2)).isTrue();
        Assertions.assertThat(mf.isStopped()).isTrue(); // no followers are
        // following
        Assertions.assertThat(mf.getMessages().size()).isEqualTo(2);
        mf.separate(f2);
        Assertions.assertThat(mf.isStopped()).isTrue(); // no followers are
        // following
        Assertions.assertThat(mf.getMessages().size()).isEqualTo(0);
        // none of these changes should have affected the second merge
        Assertions.assertThat(mf2.getMessages().size()).isEqualTo(2);
        mf2.separate(f2);
        this.getLogWatch().stopFollowing(f);
        Assertions.assertThat(mf2.getMessages().size()).isEqualTo(2);
        mf2.separate(f);
        Assertions.assertThat(mf2.getMessages().size()).isEqualTo(1);
        this.getLogWatch().terminate();
        Assertions.assertThat(mf.isStopped()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeWithSelf() {
        final Follower f = this.getLogWatch().startFollowing();
        f.mergeWith(f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeWithSelf2() {
        final Follower f = this.getLogWatch().startFollowing();
        final Follower f2 = this.getLogWatch().startFollowing();
        final MergingFollower mf = f.mergeWith(f2);
        mf.mergeWith(mf);
    }
}
