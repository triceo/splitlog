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
import com.github.triceo.splitlog.api.LogWatchBuilder;
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
        final LogWatch watch1 = LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile())
                .build();
        final Follower follower1 = watch1.startFollowing();
        LogWriter.write(follower1, MergingTest.MESSAGE_1);
        // write into second file
        final LogWatch watch2 = LogWatchBuilder.getDefault().watchedFile(LogWriter.createTempFile())
                .build();
        final Follower follower2 = watch2.startFollowing();
        LogWriter.write(follower2, MergingTest.MESSAGE_2);
        // merge both
        final MergingFollower merge = follower2.mergeWith(follower1);
        Assertions.assertThat(merge.getMerged()).containsOnly(follower1, follower2);
        Assertions.assertThat(merge.getMessages()).hasSize(0); // zero ACCEPTED
        LogWriter.write(follower2, MergingTest.MESSAGE_3);
        Assertions.assertThat(merge.getMessages()).hasSize(1); // one ACCEPTED
        LogWriter.write(follower1, MergingTest.MESSAGE_4);
        // third follows the first file; merge should have the message just once
        final Follower follower3 = watch1.startFollowing();
        final MergingFollower merge2 = follower3.mergeWith(merge);
        Assertions.assertThat(merge.getMerged()).containsOnly(follower1, follower2);
        Assertions.assertThat(merge2).isNotSameAs(merge);
        Assertions.assertThat(merge2.getMerged()).containsOnly(follower1, follower2, follower3);
        LogWriter.write(follower3, MergingTest.MESSAGE_5);
        Assertions.assertThat(merge.getMessages()).hasSize(3);
        Assertions.assertThat(merge2.getMessages()).hasSize(3);
        // test writing
        final List<String> messages = new LinkedList<>();
        messages.add(MergingTest.MESSAGE_1);
        messages.add(MergingTest.MESSAGE_2);
        messages.add(MergingTest.MESSAGE_4);
        try {
            final File f = LogWriter.createTempFile();
            merge2.write(new FileOutputStream(f), NoopMessageFormatter.INSTANCE);
            Assertions.assertThat(f).exists();
            final List<String> lines = FileUtils.readLines(f, "UTF-8");
            Assertions.assertThat(lines).isEqualTo(messages);
        } catch (final Exception e) {
            Assertions.fail("Couldn't write to file.");
        }
        // and now separate the second, making MESSAGE_2 disappear
        final MergingFollower merge3 = merge2.remove(follower2);
        Assertions.assertThat(merge2.getMerged()).contains(follower2);
        Assertions.assertThat(merge3.getMerged()).doesNotContain(follower2);
        Assertions.assertThat(merge3.getMessages()).hasSize(2);
        // now only MESSAGE_4 remains ACCEPTED
        final MergingFollower merge4 = merge3.remove(follower1);
        Assertions.assertThat(merge2.getMerged()).contains(follower1);
        Assertions.assertThat(merge4.getMerged()).doesNotContain(follower1);
        Assertions.assertThat(merge4.getMessages()).hasSize(1);
        // and now stop the followers to see what happens to the merges
        follower3.stop();
        Assertions.assertThat(follower3.isStopped()).isTrue();
        Assertions.assertThat(merge4.isStopped()).isTrue();
        Assertions.assertThat(merge3.isStopped()).isFalse();
        Assertions.assertThat(merge2.isStopped()).isFalse();
        Assertions.assertThat(merge.isStopped()).isFalse();
        // first watch is terminated, second one remains alive
        watch1.stop();
        Assertions.assertThat(follower1.isStopped()).isTrue();
        Assertions.assertThat(merge4.isStopped()).isTrue();
        Assertions.assertThat(merge3.isStopped()).isTrue();
        Assertions.assertThat(merge2.isStopped()).isFalse();
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
        LogWriter.write(f, "test");
        LogWriter.write(f2, "test2");
        Assertions.assertThat(mf.getMessages()).hasSize(1);
        // add third follower, will only receive the second message
        final Follower f3 = this.getLogWatch().startFollowing();
        LogWriter.write(f3, "test3");
        final MergingFollower mf2 = mf.mergeWith(f3);
        Assertions.assertThat(mf2).isNotEqualTo(mf);
        Assertions.assertThat(mf.getMessages()).hasSize(2);
        Assertions.assertThat(mf2.getMessages()).hasSize(2);
        // remove both followers from first merge, verify results
        final MergingFollower mf3 = mf.remove(f);
        Assertions.assertThat(mf3.getMessages()).hasSize(2);
        Assertions.assertThat(mf3.isStopped()).isFalse();
        Assertions.assertThat(this.getLogWatch().stopFollowing(f2)).isTrue();
        Assertions.assertThat(mf3.isStopped()).isTrue(); // no followers are
        // following
        Assertions.assertThat(mf.getMessages()).hasSize(2);
        Assertions.assertThat(mf3.remove(f2)).isEqualTo(null);
        // none of these changes should have affected the second merge
        Assertions.assertThat(mf2.getMessages()).hasSize(2);
        final MergingFollower mf5 = mf2.remove(f2);
        this.getLogWatch().stopFollowing(f);
        Assertions.assertThat(mf5.getMessages()).hasSize(2);
        final MergingFollower mf6 = mf5.remove(f);
        Assertions.assertThat(mf6.getMessages()).hasSize(1);
        this.getLogWatch().stop();
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
