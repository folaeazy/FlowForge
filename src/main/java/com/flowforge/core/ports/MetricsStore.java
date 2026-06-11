package com.flowforge.core.ports;

/**
 *  Defines what metrics the system tracks
 */
public interface MetricsStore {

    void incrementProcessed(String tenantId);
    void incrementFailed(String tenantId);
    void incrementRetried(String tenantId);
    void recordQueueSize(int size);

    long getProcessed(String tenantId);
    long getFailed(String tenantId);
    long getRetried(String tenantId);
}
