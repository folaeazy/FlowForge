package com.flowforge.persistence;

import com.flowforge.core.domain.ShortUrl;
import com.flowforge.core.ports.ShortUrlStore;

import java.util.List;
import java.util.Optional;

public class JpaShortUrlStore implements ShortUrlStore {
    @Override
    public Optional<ShortUrl> findByLongUrlHash(String longUrlHash) {
        return Optional.empty();
    }

    @Override
    public Optional<ShortUrl> findByShortCode(String shortCode) {
        return Optional.empty();
    }

    @Override
    public ShortUrl save(ShortUrl shortUrl) {
        return null;
    }

    @Override
    public List<ShortUrl> findRecentByTenant(String tenantId, int page, int size) {
        return List.of();
    }
}
