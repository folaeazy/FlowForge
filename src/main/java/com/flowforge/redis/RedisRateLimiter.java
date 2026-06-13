package com.flowforge.redis;


import com.flowforge.core.domain.RateLimitResult;
import com.flowforge.core.ports.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Distributed rate limiter — Redis is the single source of truth
 * for token state across all app instances.
 *
 * Responsibilities:
 *   1. Manage per-tenant configuration (capacity, rate) in memory
 *   2. Execute Lua script on Redis to atomically check + deduct tokens
 *   3. Translate the numeric result into a RateLimitResult
 *
 * Failure policy: FAIL-OPEN
 *   If Redis is unreachable, requests are allowed through.
 *   Reasoning: a rate limiter outage should not stop job processing.
 *   The consecutive failure counter makes this visible — not silent.
 *
 * What this class does NOT do:
 *   - Fall back to in-memory TokenBucket (would break distributed consistency)
 *   - Retry failed Redis calls (retries on a rate limiter add latency)
 *   - Cache "allow/reject" decisions (stale decisions are worse than a miss)
 */

@Component
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);
    private static final long SCALE = 1_000L;

    private final DefaultRedisScript<Long> rateLimitScript;
    private final RedisTemplate<String, String> redisTemplate;

    private final long defaultCapacity ;
    private final long defaultRatePerSeconds;

    private record TenantConfig(long capacity, long ratePerSec) {};

    private final ConcurrentHashMap<String, TenantConfig> tenantConfig = new ConcurrentHashMap<>();

    // Track consecutive redis failure
    //In a production system this feeds a metric:
    // metricsStore.gauge("ratelimiter.redis.failures", consecutiveFailures.get())
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    public RedisRateLimiter(
             DefaultRedisScript<Long> rateLimitScript,
             RedisTemplate<String, String> redisTemplate,
             @Value("${flowforge.rate-limiter.default-capacity:100}")
             long defaultCapacity,
             @Value("${flowforge.rate-limiter.default-rate-per-second:50}")
             long defaultRatePerSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
        this.defaultCapacity = defaultCapacity;
        this.defaultRatePerSeconds = defaultRatePerSeconds;

    }

    @Override
    public RateLimitResult tryAcquire(String tenantId) {
        TenantConfig config = tenantConfig.getOrDefault(
                tenantId,
                new TenantConfig(defaultCapacity, defaultRatePerSeconds)
        );

        try{
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    List.of(RedisKeys.rateLimitKey(tenantId)),
                    String.valueOf(config.capacity),
                    String.valueOf(config.ratePerSec),
                    String.valueOf(System.currentTimeMillis()),
                    "1",
                    String.valueOf(SCALE)
            );

            // Successful Redis call — reset failure counter
            consecutiveFailures.set(0);

            if(result == null) {
                // resolve to fail open -- allow request till redis is back: But most likely won't return null
                log.error("[RateLimiter] Null result from Lua script tenant={}", tenantId);
                return failOpen(tenantId, "null script result");
            }

            boolean allowed = result >= 0;
            long availableTokens = allowed ? result / SCALE : 0L;

            log.debug("[RateLimiter] tenant={} allowed={} available={}",
                    tenantId, allowed, availableTokens);

            return allowed
                    ? RateLimitResult.allowed(tenantId, availableTokens)
                    : RateLimitResult.rejected(tenantId, availableTokens);

        } catch (Exception e) {
            long failures = consecutiveFailures.incrementAndGet();

            // Log every failure, but only at ERROR level after first one —
            // avoids log spam on sustained outage while keeping first alert loud
            if (failures == 1) {
                log.error("[RateLimiter] Redis unreachable — failing open. tenant={}",
                        tenantId, e);
            } else {
                log.error("[RateLimiter] Redis still unreachable (failure #{}) — failing open. tenant={}",
                        failures, tenantId);
            }

            return failOpen(tenantId, e.getMessage());
        }
    }



    @Override
    public void registerTenant(String tenantId, long capacity, long ratePerSecond) {

    }

    @Override
    public long availableTokens(String tenantId) {
        return 0;
    }

    private RateLimitResult failOpen(String tenantId, String reason) {
        log.warn("[RateLimiter] Fail-open applied tenant={} reason={}", tenantId, reason);
        return RateLimitResult.allowed(tenantId, 0);
    }
}
