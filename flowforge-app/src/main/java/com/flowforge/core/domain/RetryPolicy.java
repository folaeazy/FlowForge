package com.flowforge.core.domain;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Calculates retry delay using exponential backoff with full jitter.
 * WHY EXPONENTIAL BACKOFF:
 *   After a failure, the downstream system likely needs time to recover.
 *   Retrying immediately hammers a struggling dependency. Doubling the
 *   delay each attempt gives it breathing room.
 *
 *      Attempt 0 → fail → wait ~1s  → retry
 *     Attempt 1 → fail → wait ~2s  → retry
 *     Attempt 2 → fail → wait ~4s  → retry (then → DLQ)
 *
 *   WHY JITTER:
 *   If 1000 jobs all fail at T=0 and all retry at T=1000ms exactly,
 *   you've just created a thundering herd at T=1000ms.
 *   Jitter randomizes the retry window, spreading load evenly.
 *
 *   FORMULA (full jitter — AWS recommended):
 *     delay = random(0, min(maxDelay, baseDelay * 2^attempt))
 */
public final class RetryPolicy {
    private final long baseDelayMs;
    private final long maxDelayMs;

    public RetryPolicy(long baseDelayMs, long maxDelayMs) {
        if(baseDelayMs <= 0 || maxDelayMs < baseDelayMs) {
            throw new IllegalArgumentException("invalid delay config");
        }

        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
    }
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(1_000L, 30_000L); // 1s base 30s max
    }

    /**
     * Calculate delay for the Nth retry attempt.
     *
     * @param attemptNumber  0-based (0 = first retry after first failure)
     */
    public long delayMs(int attemptNumber) {
        // 2^attempt can overflow int quickly — cap at 30 to prevent overflow
        int cappedAttempt = Math.min(attemptNumber, 30);

        // ceiling before jitter
        long ceiling = Math.min(maxDelayMs, baseDelayMs * (1L << cappedAttempt));

        // full jitter: random in [0, ceiling]
        // ThreadLocalRandom is faster than Random under concurrency — no shared state
        return ThreadLocalRandom.current().nextLong(0, ceiling + 1);
    }

    @Override
    public String toString() {
        return String.format("RetryPolicy[base=%dms, max=%dms]", baseDelayMs, maxDelayMs);
    }

}
