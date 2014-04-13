package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.MergingFollower;

public class MergingTest extends DefaultFollowerBaseTest {

    @Test(expected = IllegalArgumentException.class)
    public void testMergeWithNull() {
        final Follower f = this.getLogWatch().startFollowing();
        f.mergeWith(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeWithSelf() {
        final Follower f = this.getLogWatch().startFollowing();
        f.mergeWith(f);
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
        Assertions.assertThat(mf.isFollowing()).isTrue();
        Assertions.assertThat(this.getLogWatch().stopFollowing(f2)).isTrue();
        Assertions.assertThat(mf.isFollowing()).isFalse(); // no followers are
                                                           // following
        Assertions.assertThat(mf.getMessages().size()).isEqualTo(2);
        mf.separate(f2);
        Assertions.assertThat(mf.isFollowing()).isFalse(); // no followers are
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
        Assertions.assertThat(mf.isFollowing()).isFalse();
    }
}
