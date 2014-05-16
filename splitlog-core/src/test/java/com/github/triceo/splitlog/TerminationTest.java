package com.github.triceo.splitlog;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.triceo.splitlog.api.Follower;
import com.github.triceo.splitlog.api.LogWatchBuilder;

@RunWith(Parameterized.class)
public class TerminationTest extends DefaultFollowerBaseTest {

    public TerminationTest(final LogWatchBuilder builder) {
        super(builder);
    }

    @Test
    public void testTermination() {
        Assertions.assertThat(this.getLogWatch().isStopped()).as("Log watch terminated immediately after starting.")
        .isFalse();
        final Follower follower1 = this.getLogWatch().startFollowing();
        Assertions.assertThat(this.getLogWatch().isFollowedBy(follower1))
        .as("Follower terminated immediately after starting.").isTrue();
        final Follower follower2 = this.getLogWatch().startFollowing();
        Assertions.assertThat(this.getLogWatch().stopFollowing(follower1)).as("Wrong termination result.").isTrue();
        Assertions.assertThat(this.getLogWatch().stopFollowing(follower1)).as("Wrong termination result.").isFalse();
        Assertions.assertThat(this.getLogWatch().isFollowedBy(follower2))
        .as("Follower terminated without termination.").isTrue();
        Assertions.assertThat(this.getLogWatch().isFollowedBy(follower1))
        .as("Follower not terminated after termination.").isFalse();
        Assertions.assertThat(this.getLogWatch().stop()).as("Wrong termination result.").isTrue();
        Assertions.assertThat(this.getLogWatch().stop()).as("Wrong termination result.").isFalse();
        Assertions.assertThat(this.getLogWatch().isFollowedBy(follower2))
        .as("Follower not terminated after termination.").isFalse();
        Assertions.assertThat(this.getLogWatch().isStopped()).as("Log watch not terminated after termination.")
        .isTrue();
    }

}
