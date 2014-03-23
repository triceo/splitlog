package com.github.triceo.splitlog;

import org.junit.Assert;
import org.junit.Test;

public class MergingTest extends DefaultFollowerBaseTest {

    @Test(expected = IllegalArgumentException.class)
    public void testMergeWithNull() {
        final Follower f = this.getLogWatch().follow();
        f.mergeWith(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeWithSelf() {
        final Follower f = this.getLogWatch().follow();
        f.mergeWith(f);
    }

    @Test
    public void testMergeWithSameLogWatch() {
        final Follower f = this.getLogWatch().follow();
        final Follower f2 = this.getLogWatch().follow();
        final MergingFollower mf = f.mergeWith(f2);
        // send the message; the merged should still contain only the one
        this.getWriter().write("test", f);
        this.getWriter().write("test2", f2);
        Assert.assertEquals(1, mf.getMessages().size());
        // add third follower, will only receive the second message
        final Follower f3 = this.getLogWatch().follow();
        this.getWriter().write("test3", f3);
        final MergingFollower mf2 = mf.mergeWith(f3);
        Assert.assertNotEquals(mf, mf2);
        Assert.assertEquals(2, mf.getMessages().size());
        Assert.assertEquals(2, mf2.getMessages().size());
        // remove both followers from first merge, verify results
        mf.separate(f);
        Assert.assertEquals(2, mf.getMessages().size());
        Assert.assertTrue(mf.isFollowing());
        Assert.assertTrue(this.getLogWatch().unfollow(f2));
        Assert.assertFalse(mf.isFollowing()); // no followers are following
        Assert.assertEquals(2, mf.getMessages().size());
        mf.separate(f2);
        Assert.assertFalse(mf.isFollowing()); // no followers are following
        Assert.assertEquals(0, mf.getMessages().size());
        // none of these changes should have affected the second merge
        Assert.assertEquals(2, mf2.getMessages().size());
        mf2.separate(f2);
        this.getLogWatch().unfollow(f);
        Assert.assertEquals(2, mf2.getMessages().size());
        mf2.separate(f);
        Assert.assertEquals(1, mf2.getMessages().size());
        this.getLogWatch().terminate();
        Assert.assertFalse(mf.isFollowing());
    }
}
