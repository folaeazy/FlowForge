package com.flowforge.api.controller;

import com.flowforge.core.domain.ShortUrl;
import com.flowforge.core.ports.ShortUrlStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/urls")
public class UrlController {
    private final ShortUrlStore shortUrlStore;

    public UrlController(ShortUrlStore shortUrlStore) {
        this.shortUrlStore = shortUrlStore;
    }

    @GetMapping
    public List<ShortUrlResponse> list(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return shortUrlStore.findRecentByTenant(tenantId, page, size)
                .stream()
                .map(ShortUrlResponse::from)
                .toList();
    }

    public record ShortUrlResponse(String shortCode, String longUrl, String tenantId, Instant createdAt, long hitCount) {
        public static ShortUrlResponse from(ShortUrl s) {
            return new ShortUrlResponse(s.shortCode(), s.longUrl(), s.tenantId(), s.createdAt(), s.hitCount());
        }
    }
}
