package com.pks.p2p.util;

public class TimeOutCondition implements Condition {
    private final long startTime;
    private final long timeout;

    public TimeOutCondition(long timeout) {
        this.startTime = System.currentTimeMillis();
        this.timeout = timeout;
    }

    @Override
    public boolean compute() {
        return System.currentTimeMillis() - startTime < timeout;
    }
}
