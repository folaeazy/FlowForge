package com.flowforge.redis;

import com.flowforge.core.ports.MetricsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

/**
 *   HASH — for per-tenant counters:
 *       flowforge:metrics:processed  → {tenant-A: 1200, tenant-B: 400}
 *       flowforge:metrics:failed     → {tenant-A: 12, tenant-B: 5}
 *       flowforge:metrics:retries    → {tenant-A: 3}
 *
 *       WHY HASH (not separate STRING per tenant):
 *       One HGETALL call fetches ALL tenant metrics at once.
 *       Separate STRING keys would require one GET per tenant.
 *       Dashboard loads all tenants in a single Redis round-trip.
 */
public class RedisMetricsStore implements MetricsStore {

    private static final Logger log = LoggerFactory.getLogger(RedisMetricsStore.class);

    // How long to keep per-minute TPS buckets
    private static final Duration TPS_BUCKET_TTL = Duration.ofHours(2);

    private final RedisTemplate<String, String> redisTemplate;

    public RedisMetricsStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void incrementProcessed(String tenantId) {
        safeIncrement(RedisKeys.METRICS_PROCESSED, tenantId);
        incrementTpsBucket("success");

    }

    @Override
    public void incrementFailed(String tenantId) {
        safeIncrement(RedisKeys.METRICS_FAILED ,tenantId);
        incrementTpsBucket("failed");
    }

    @Override
    public void incrementRetried(String tenantId) {
        safeIncrement(RedisKeys.METRICS_RETRIES , tenantId);
    }

    @Override
    public void recordQueueSize(int size) {
        try {
            redisTemplate.opsForValue().set(RedisKeys.QUEUE_SIZE, String.valueOf(size));
        } catch (Exception e) {
            log.error("Failed to record queue size", e);
        }

    }

    @Override
    public long getProcessed(String tenantId) {
        return safeRead(RedisKeys.METRICS_PROCESSED, tenantId);
    }

    @Override
    public long getFailed(String tenantId) {
        return safeRead(RedisKeys.METRICS_FAILED, tenantId);
    }

    @Override
    public long getRetried(String tenantId) {
        return safeRead(RedisKeys.METRICS_RETRIES, tenantId);
    }


    //======================private helpers=========================//


    /**
     * HINCRBY — atomically increments a hash field.
     * Creates both the hash and the field if they don't exist.
     */
    private void safeIncrement(String hashKey, String field) {
        try {
            redisTemplate.opsForHash().increment(hashKey, field, 1);
        } catch (Exception e) {
            log.error("Failed to increment metric hashKey={} field={}", hashKey, field, e);
        }

    }

    private long safeRead(String hashKey, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(hashKey, field);
            return value == null ? 0L : Long.parseLong(value.toString());
        } catch (Exception e) {
            log.error("Failed to read metric hashKey={} field={}", hashKey, field, e);
            return 0L;
        }
    }


    /**
     * Increment the current-minute TPS bucket.
     * Key is time-bucketed — changes automatically every minute.
     * EXPIRE is reset on each write to keep the bucket alive
     * while data is actively flowing in.
     */
    private void incrementTpsBucket(String type) {
        try {
            String key = RedisKeys.tpsBucket(type);
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, TPS_BUCKET_TTL);
        } catch (Exception e) {
            log.error("Failed to increment TPS bucket type={}", type, e);
        }
    }
}
