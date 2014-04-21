package com.github.triceo.splitlog;

import java.io.File;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatch;
import com.github.triceo.splitlog.api.MergingFollower;

public class AbstractMergingFollowerTest {

    private static final String MESSAGE_1 = "Message";
    private static final String MESSAGE_2 = "Second Message";
    private static final String MESSAGE_3 = "Third Message";
    private static final String MESSAGE_4 = "Fourth Message";
    private static final String MESSAGE_5 = "Fifth Message";

    private static File createTempFile() {
        try {
            return File.createTempFile("splitlog-", ".tmp");
        } catch (final IOException e) {
            Assertions.fail("Couldn't create file.", e);
            return null;
        }
    }

    @Test
    public void testMerging() {
        // write into first file
        final LogWatch watch1 = LogWatchBuilder.forFile(AbstractMergingFollowerTest.createTempFile()).build();
        final Follower follower1 = watch1.startFollowing();
        final LogWriter writer1 = LogWriter.forFile(watch1.getWatchedFile());
        writer1.write(AbstractMergingFollowerTest.MESSAGE_1, follower1);
        // write into second file
        final LogWatch watch2 = LogWatchBuilder.forFile(AbstractMergingFollowerTest.createTempFile()).build();
        final Follower follower2 = watch2.startFollowing();
        final LogWriter writer2 = LogWriter.forFile(watch2.getWatchedFile());
        writer2.write(AbstractMergingFollowerTest.MESSAGE_2, follower2);
        // merge both
        final MergingFollower merge = follower2.mergeWith(follower1);
        Assertions.assertThat(merge.getMerged()).containsOnly(follower1, follower2);
        Assertions.assertThat(merge.getMessages()).hasSize(0); // zero ACCEPTED
        writer2.write(AbstractMergingFollowerTest.MESSAGE_3, follower2);
        Assertions.assertThat(merge.getMessages()).hasSize(1); // one ACCEPTED
        writer1.write(AbstractMergingFollowerTest.MESSAGE_4, follower1);
        // third follows the first file; merge should have the message just once
        final Follower follower3 = watch1.startFollowing();
        final MergingFollower merge2 = merge.mergeWith(follower3);
        Assertions.assertThat(merge.getMerged()).containsOnly(follower1, follower2);
        Assertions.assertThat(merge2).isNotSameAs(merge);
        Assertions.assertThat(merge2.getMerged()).containsOnly(follower1, follower2, follower3);
        writer1.write(AbstractMergingFollowerTest.MESSAGE_5, follower3);
        Assertions.assertThat(merge.getMessages()).hasSize(3);
        Assertions.assertThat(merge2.getMessages()).hasSize(3);
        // and now separate the second, making MESSAGE_2 disappear
        Assertions.assertThat(merge2.separate(follower2)).isTrue();
        Assertions.assertThat(merge2.getMessages()).hasSize(2);
        // now only MESSAGE_4 remains ACCEPTED
        Assertions.assertThat(merge2.separate(follower1)).isTrue();
        Assertions.assertThat(merge2.getMessages()).hasSize(1);
    }
}
