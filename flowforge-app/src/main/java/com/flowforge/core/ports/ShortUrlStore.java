package com.flowforge.core.ports;

import com.flowforge.core.domain.ShortUrl;

import java.util.List;
import java.util.Optional;

public interface ShortUrlStore {

    Optional<ShortUrl> findByLongUrlHash(String longUrlHash);

    Optional<ShortUrl> findByShortCode(String shortCode);

    /**
     * Attempts to persist a new mapping. Throws DuplicateShortCodeException
     * if short_code collides (caller retries with a new code). If longUrlHash
     * collides instead — a concurrent job for the same URL won the race —
     * implementations should let that exception propagate too; the caller
     * (UrlShortenerJobProcessor) re-queries findByLongUrlHash to adopt the
     * winner's code rather than treating it as a failure.
     */
    ShortUrl save(ShortUrl shortUrl);

    /** Paginated, newest first — backs the dashboard's Jobs/Recent URLs view. */
    List<ShortUrl> findRecentByTenant(String tenantId, int page, int size);
}
