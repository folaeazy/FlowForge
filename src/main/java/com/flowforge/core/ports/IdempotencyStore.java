package com.flowforge.core.ports;

/**
 * Port — defines idempotency contract.
 *
 * An idempotency store answers one question:
 * "Has this job already been successfully processed?"
 *
 */
public interface IdempotencyStore {

    /**
     * Mark job as processed
     *
     * @param jobId unique job identity
     * @param ttlSeconds how long to remember job
     */
    void markProcessed(String jobId, long ttlSeconds);


    /**
     * Check if a job was already successfully processed.
     *
     * @return true if job should be skipped (already done)
     */
    boolean isProcessed(String jobId);
}
