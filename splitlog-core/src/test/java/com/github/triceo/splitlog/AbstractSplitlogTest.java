package com.github.triceo.splitlog;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.rules.Timeout;

/**
 * Base class for all Splitlog tests. Will enforce a default timeout.
 */
public abstract class AbstractSplitlogTest {

    @Rule
    public Timeout globalTimeout = new Timeout((int) TimeUnit.MINUTES.toMillis(2));

}
