package com.flowforge.redis;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Central registry of all Redis key patterns used by FlowForge.
 * class creates a key that nothing else can find — silent data loss.
 * One class, one source of truth.
 *
 * What keys look like in redis-cli:
 *  flowforge:ratelimit:tenant-A         HASH  {tokens, last_refill}
 *  flowforge:idempotency:job-uuid-123   STRING "1"
 *  flowforge:metrics:processed          HASH  {tenant-A: 42, tenant-B: 17}
 */
public final class RedisKeys {

    private static final String PREFIX = "flowforge:";
    private static final DateTimeFormatter MINUTE_BUCKET = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    //--------Rate limiter ----------//

    /**
     *
     * Per-tenant rate limit state.
     * Stored as a Redis HASH: {tokens: "85000", last_refill: "1736950412000"}
     */
    public static String rateLimitKey(String tenantId) {
        return PREFIX + "ratelimit:"  + tenantId;

    }

    // ── Idempotency ──────────────────────────────────────────────────────────

    /**
     * Marks a job as successfully processed.
     * Existence of this key = job is done, skip it.
     * Value is "1" — the value doesn't matter, existence does.
     */
    public static String idempotencyKey(String jobId) {
        return PREFIX + "idempotency:" + jobId;
    }
    public static final String PREFIX_TPS_SUCCESS = PREFIX + "tps:success:";
    public static final String PREFIX_TPS_FAILED  = PREFIX + "tps:failed:";
   // ─────Metrics  ──────────────────────────────────────────────────────────

    public static final String METRICS_PROCESSED = PREFIX + "metrics:processed";
    public static final String METRICS_FAILED    = PREFIX + "metrics:failed";
    public static final String METRICS_RETRIES   = PREFIX + "metrics:retries";

    // Simple STRING — overwritten on each queue size snapshot
    public static final String QUEUE_SIZE = PREFIX + "metrics:queue_size";

    /**
     * Per-minute TPS bucket.
     * Key changes every minute — gives us time-series data for the chart.
     * Old buckets expire automatically (TTL set on write).
     *
     * e.g., "flowforge:tps:success:202501151437"
     */
    public static String tpsBucket(String type) {
        String prefix = type.equals("success") ? PREFIX_TPS_SUCCESS : PREFIX_TPS_FAILED;
        return prefix + LocalDateTime.now().format(MINUTE_BUCKET);
    }

    // Prevent instantiation — this is a utility class
    private RedisKeys() {}

}
