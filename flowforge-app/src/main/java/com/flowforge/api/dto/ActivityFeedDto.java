package com.flowforge.api.dto;

import java.time.Instant;

public record ActivityFeedDto(
        Instant timestamp,
        String type,
        String subject,
        String jobId,
        String message
) {
    public ActivityFeedDto {
        // validation in serialization
        if (timestamp == null) throw new IllegalArgumentException("timestamp required");
        if (type == null) throw new IllegalArgumentException("type required");
        if (subject == null) throw new IllegalArgumentException("subject required");
        if (jobId == null) throw new IllegalArgumentException("jobId required");
        if (message == null) message = ""; // allow empty, not null
    }
}
