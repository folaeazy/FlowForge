package com.flowforge.engine;

import com.flowforge.core.ports.MetricsStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FakeMetricsStore implements MetricsStore {

    private final Map<String, AtomicLong> processed = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failed = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> retried = new ConcurrentHashMap<>();

    public void incrementProcessed(String t) {
        processed.computeIfAbsent(t, k -> new AtomicLong()).incrementAndGet();
    }
    public void incrementFailed(String t)    {
        failed.computeIfAbsent(t, k -> new AtomicLong()).incrementAndGet();
    }
    public void incrementRetried(String t)   {
        retried.computeIfAbsent(t, k -> new AtomicLong()).incrementAndGet();
    }
    public void recordQueueSize(int size)    {
        /* not needed for these tests */
    }
    public long getProcessed(String t) {
        return processed.getOrDefault(t, new AtomicLong()).get();
    }
    public long getFailed(String t)    {
        return failed.getOrDefault(t, new AtomicLong()).get();
    }
    public long getRetried(String t)   {
        return retried.getOrDefault(t, new AtomicLong()).get();
    }
}
