package com.flowforge.core.domain;

import java.time.Instant;

public record ShortUrl (
        Long id,
        String shortCode,
        String longUrl,
        String longUrlHash,
        String tenantId,
        String jobId,
        Instant createdAt

){
}
