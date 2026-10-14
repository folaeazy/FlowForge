package com.flowforge.api.controller;

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

    public RedirectController(ShortUrlStore shortUrlStore, StringRedisTemplate redisTemplate) {
        this.shortUrlStore = shortUrlStore;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/{shortCode:[0-9A-Za-z]{7}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;
        String longUrl = redisTemplate.opsForValue().get(cacheKey);

        if(longUrl == null) {
            Optional<ShortUrl> found = shortUrlStore.findByShortCode(shortCode);
            if(found.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            longUrl = found.get().longUrl();
            // Add to cache
            redisTemplate.opsForValue().set(cacheKey, longUrl, CACHE_TTL);
        }
        shortUrlStore.incrementHitCount(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
