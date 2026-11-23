package com.flowforge.components;

import com.flowforge.core.ports.ShortUrlStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HitCounter {

    private static final Logger log = LoggerFactory.getLogger(HitCounter.class);
    private static final String HITS_PREFIX = "shorturl:hits:";
    private static final String DIRTY_SET = "shorturl:hits:dirty";
    private static final int FLUSH_BATCH = 500;

    private final StringRedisTemplate redis;
    private final ShortUrlStore store;

    public HitCounter(StringRedisTemplate redis, ShortUrlStore store) {
        this.redis = redis;
        this.store = store;
    }

    public void record(String shortCode) {
        redis.opsForValue().increment(HITS_PREFIX + shortCode);
        redis.opsForSet().add(DIRTY_SET, shortCode);
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        List<String> codes = redis.opsForSet().pop(DIRTY_SET, FLUSH_BATCH);
        if (codes == null) return;
        for (String code : codes) {
            String delta = redis.opsForValue().getAndDelete(HITS_PREFIX + code);
            if (delta == null) continue;
            try {
                store.addHits(code, Long.parseLong(delta));
            } catch (Exception e) {
                log.warn("Hit flush failed for {}, re-queueing: {}", code, e.getMessage());
                redis.opsForValue().increment(HITS_PREFIX + code, Long.parseLong(delta));
                redis.opsForSet().add(DIRTY_SET, code);
            }
        }
    }
}
