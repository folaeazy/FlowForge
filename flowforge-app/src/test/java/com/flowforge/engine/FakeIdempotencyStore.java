package com.flowforge.engine;

import com.flowforge.core.ports.IdempotencyStore;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FakeIdempotencyStore implements IdempotencyStore {

    private final Set<String> processed = ConcurrentHashMap.newKeySet();
    public void markProcessed(String jobId, long ttlSeconds) {
        processed.add(jobId);
    }

    public boolean isProcessed(String jobId) {
        return processed.contains(jobId);
    }
}
