package com.flowforge.api.dto;

import com.flowforge.core.domain.Job;
import com.flowforge.core.domain.JobStatus;

import java.time.Instant;

public record DlqEntryResponse(
        String jobId,
        String tenantId,
        String type,
        int attemptCount,
        int maxRetries,
        JobStatus status,
        Instant createdAt,
        Instant lastAttemptAt

) {
    public static DlqEntryResponse from(Job job) {
        return new DlqEntryResponse(
                job.getJobId(), job.getTenantId(), job.getType(),
                job.getAttemptCount(), job.getMaxRetries(), job.getStatus(),
                job.getCreatedAt(), job.getLastAttemptAt()
        );
    }
}
