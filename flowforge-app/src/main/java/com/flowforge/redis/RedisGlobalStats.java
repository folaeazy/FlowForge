package com.flowforge.redis;

import com.flowforge.core.ports.GlobalStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RedisGlobalStats implements GlobalStats {

    private static final Logger log = LoggerFactory.getLogger(RedisGlobalStats.class);
    private final StringRedisTemplate redis;

    public RedisGlobalStats(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void incrementAdmitted() {
        bump("admitted");
    }

    @Override
    public void incrementRateLimited(String identity) {
        bump("rateLimited");
        try {
            redis.opsForZSet().incrementScore(RedisKeys.THROTTLED_IDENTITIES, identity, 1);
        } catch (Exception e) {
            log.warn("[Stats] Could not record throttled identity: {}", e.getMessage());
        }

    }

    @Override
    public void incrementProcessed() {
        bump("processed");
    }

    @Override
    public void incrementFailed() {
        bump("failed");
    }

    @Override
    public void incrementRetried() {
        bump("retried");
    }

    @Override
    public void incrementDead() {
        bump("dead");
    }

    @Override
    public Snapshot snapshot() {
        try {
            Map<Object, Object> h = redis.opsForHash().entries(RedisKeys.GLOBAL_STATS);
            return new Snapshot(read(h, "admitted"), read(h, "rateLimited"), read(h, "processed"),
                    read(h, "failed"), read(h, "retried"), read(h, "dead"));
        } catch (Exception e) {
            log.warn("[Stats] Could not read snapshot: {}", e.getMessage());
            return new Snapshot(0, 0, 0, 0, 0, 0);
        }
    }

    @Override
    public List<ThrottledIdentity> topThrottled(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<String>> top =
                    redis.opsForZSet().reverseRangeWithScores(RedisKeys.THROTTLED_IDENTITIES, 0, limit - 1);
            if (top == null) return List.of();
            return top.stream()
                    .map(t -> new ThrottledIdentity(t.getValue(), t.getScore() == null ? 0 : t.getScore().longValue()))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }


    // Stats must never break the request path, so failures are logged and swallowed (same fail-open stance as the limiter).
    private void bump(String field) {
        try {
            redis.opsForHash().increment(RedisKeys.GLOBAL_STATS, field, 1);
        } catch (Exception e) {
            log.warn("[Stats] Could not record {}: {}", field, e.getMessage());
        }
    }

    private long read(Map<Object, Object> h, String field) {
        Object v = h.get(field);
        return v == null ? 0 : Long.parseLong(v.toString());
    }
}
