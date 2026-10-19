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
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        List<ShortUrl> rows = (tenantId == null || tenantId.isBlank())
                ? shortUrlStore.findRecent(safePage, safeSize)
                : shortUrlStore.findRecentByTenant(tenantId, safePage, safeSize);
        return rows.stream().map(ShortUrlResponse::from).toList();
    }

    public record ShortUrlResponse(String shortCode, String longUrl, String tenantId, Instant createdAt, long hitCount) {
        public static ShortUrlResponse from(ShortUrl s) {
            return new ShortUrlResponse(s.shortCode(), s.longUrl(), s.tenantId(), s.createdAt(), s.hitCount());
        }
    }
}
