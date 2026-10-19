package com.flowforge.core.ports;

import reactor.core.scheduler.Schedulers;

import java.util.List;

public interface GlobalStats {

    void incrementAdmitted();
    void incrementRateLimited(String identity);
    void incrementProcessed();
    void incrementFailed();
    void incrementRetried();
    void incrementDead();

    Snapshot snapshot();

    List<ThrottledIdentity> topThrottled(int limit);

    record Snapshot(long admitted, long rateLimited, long processed, long failed, long retried, long dead) {}
    record ThrottledIdentity(String identity, long rejections) {}
}
