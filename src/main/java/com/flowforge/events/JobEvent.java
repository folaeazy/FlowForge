package com.flowforge.events;

import java.time.Instant;

/**
 *  Sealed hierarchy of job lifecycle events.
 */
public sealed interface JobEvent {
    String jobId();
    String tenantId();
    Instant timestamp();

    record JobProcessingStarted(
            String jobId, String tenantId, String workerId, int attempt, Instant timestamp
    ) implements JobEvent {}

    record JobCompleted(
            String jobId, String tenantId, String workerId, Instant timestamp
    ) implements JobEvent {}

    /** Distinct from JobCompleted — lets the dashboard show "skipped duplicate" honestly. */
    record JobSkippedDuplicate(
            String jobId, String tenantId, Instant timestamp
    ) implements JobEvent {}

    record JobFailed(
            String jobId, String tenantId, String workerId, String reason, int attempt, Instant timestamp
    ) implements JobEvent {}

    record JobRetryScheduled(
            String jobId, String tenantId, long delayMs, int nextAttempt, Instant timestamp
    ) implements JobEvent {}

    record JobDead(
            String jobId, String tenantId, String reason, Instant timestamp
    ) implements JobEvent {}
}
