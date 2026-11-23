package com.flowforge.api.controller;

import com.flowforge.components.HitCounter;
import com.flowforge.core.domain.ShortUrl;
import com.flowforge.core.ports.ShortUrlStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@RestController
public class RedirectController {

    private static final String CACHE_PREFIX = "shorturl:cache:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final ShortUrlStore shortUrlStore;
    private final StringRedisTemplate redisTemplate;
    private final HitCounter hitCounter;

    public RedirectController(ShortUrlStore shortUrlStore, StringRedisTemplate redisTemplate, HitCounter hitCounter) {
        this.shortUrlStore = shortUrlStore;
        this.redisTemplate = redisTemplate;
        this.hitCounter = hitCounter;
    }

    @GetMapping("/{shortCode:[0-9A-Za-z]{7}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;
        String longUrl = redisTemplate.opsForValue().get(cacheKey);
        boolean cacheHit = longUrl != null;

        if(!cacheHit) {
            Optional<ShortUrl> found = shortUrlStore.findByShortCode(shortCode);
            if(found.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            longUrl = found.get().longUrl();
            // Add to cache
            redisTemplate.opsForValue().set(cacheKey, longUrl, CACHE_TTL);
        }
        hitCounter.record(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("X-Cache", cacheHit ? "HIT" : "MISS")
                .location(URI.create(longUrl))
                .build();
    }
}
