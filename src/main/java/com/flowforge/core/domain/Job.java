package com.flowforge.core.domain;


import java.time.Instant;
import java.util.*;

/**
 * Core domain Object -IMMUTABLE
 *
 */
public final class Job {

    private final String jobId;
    private final String tenantId;
    private final String type;           // e.g., "EMAIL_SEND", "REPORT_GENERATE"
    private final Map<String, Object> payload;
    private final int maxRetries;
    private final int attemptCount;
    private final JobStatus status;
    private final Instant createdAt;
    private final Instant lastAttemptAt;

    private Job(Builder builder) {
       this.jobId = builder.jobId;
        this.tenantId      = builder.tenantId;
        this.type          = builder.type;
        this.payload       = Map.copyOf(builder.payload); // defensive copy — payload is untrusted
        this.maxRetries    = builder.maxRetries;
        this.attemptCount  = builder.attemptCount;
        this.status        = builder.status;
        this.createdAt     = builder.createdAt;
        this.lastAttemptAt = builder.lastAttemptAt;

    }

    //--------------------------State transition methods------------------------//

    /** Call when a worker picks this job up */
    public Job markProcessing() {
        return toBuilder()
                .attemptCount(this.attemptCount + 1)
                .status(JobStatus.PROCESSING)
                .lastAttemptAt(Instant.now())
                .build();
    }

    /** Call when processing succeeds */
    public Job markCompleted() {
        return toBuilder().status(JobStatus.COMPLETED).build();
    }

    /** Call when processing fails but retries remain */
    public Job markFailed() {
        return toBuilder().status(JobStatus.FAILED).build();
    }

    /** Call when retries are exhausted — terminal state */
    public Job markDead() {
        return toBuilder().status(JobStatus.DEAD).build();
    }

    public boolean hasRetriesLeft() {
        return attemptCount < maxRetries;
    }


    /**  Getters */

    public String getJobId() {
        return jobId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }






    //-------------------------------------------BUILDER---------------------------------//
    private Builder toBuilder() {
        return new Builder()
                .jobId(jobId)
                .tenantId(tenantId)
                .type(type)
                .payload(payload)
                .maxRetries(maxRetries)
                .attemptCount(attemptCount)
                .status(status)
                .createdAt(createdAt)
                .lastAttemptAt(lastAttemptAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String jobId = UUID.randomUUID().toString(); // default: auto-generate
        private String tenantId;
        private String type;
        private Map<String, Object> payload = Map.of();
        private int maxRetries = 3;                          // sensible default
        private int attemptCount = 0;
        private JobStatus status = JobStatus.PENDING;
        private Instant createdAt = Instant.now();
        private Instant lastAttemptAt = null;

        public Builder jobId(String jobId)               { this.jobId = jobId; return this; }
        public Builder tenantId(String tenantId)         { this.tenantId = tenantId; return this; }
        public Builder type(String type)                 { this.type = type; return this; }
        public Builder payload(Map<String, Object> p)    { this.payload = p; return this; }
        public Builder maxRetries(int r)                 { this.maxRetries = r; return this; }
        public Builder attemptCount(int a)               { this.attemptCount = a; return this; }
        public Builder status(JobStatus s)               { this.status = s; return this; }
        public Builder createdAt(Instant t)              { this.createdAt = t; return this; }
        public Builder lastAttemptAt(Instant t)          { this.lastAttemptAt = t;  return this; }

        public Job build() {
            Objects.requireNonNull(tenantId, "tenantId is required");
            Objects.requireNonNull(type, "type is required");
            if (maxRetries < 0) throw new IllegalArgumentException("maxRetries must be >= 0");
            return new Job(this);
        }

    }



    @Override
    public String toString() {
        return String.format("Job[id=%s, tenant=%s, type=%s, status=%s, attempt=%d/%d]",
                jobId, tenantId, type, status, attemptCount, maxRetries);

    }




}
