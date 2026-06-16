package com.flowforge.redis;

import com.flowforge.core.ports.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyStore.class);
    private static final long DEFAULT_TTL_SECONDS = 86_400L; // 24 hours

    private final RedisTemplate<String, String> redisTemplate;

    public RedisIdempotencyStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void markProcessed(String jobId, long ttlSeconds) {
        String key = RedisKeys.idempotencyKey(jobId);
        try {
            redisTemplate.opsForValue().setIfAbsent(
                    key,
                    "1",
                    Duration.ofSeconds(ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS)
            );
            log.debug("Marked job as processed jobId={}", jobId);
        }catch (Exception e) {
            log.error("Failed to mark job as processed jobId={}", jobId, e);
        }

    }

    @Override
    public boolean isProcessed(String jobId) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(RedisKeys.idempotencyKey(jobId))
            );
        } catch (Exception e) {
            // Redis unavailable — fail open (assume not processed).
            // Risk: duplicate processing. Acceptable vs blocking all jobs.
            log.error("Failed to check idempotency for jobId={} — assuming not processed", jobId, e);
            return false;
        }
    }
}
