package com.github.triceo.splitlog;

public interface Tailable {

    boolean isTerminated();

    Tailable startTailing();

    boolean terminateTailing();

}
