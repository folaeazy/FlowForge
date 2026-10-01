package com.flowforge.persistence;


import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "short_urls")
public class ShortUrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true)
    private String shortCode;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Column(name = "long_url_hash", nullable = false, unique = true, columnDefinition = "CHAR(64)")
    private String longUrlHash;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "hit_count", nullable = false)
    private long hitCount = 0;


    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ShortUrlEntity() {} // required by JPA

    public ShortUrlEntity(String shortCode, String longUrl, String longUrlHash,
                          String tenantId, String jobId, Instant createdAt) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.longUrlHash = longUrlHash;
        this.tenantId = tenantId;
        this.jobId = jobId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public String getLongUrl() { return longUrl; }
    public String getLongUrlHash() { return longUrlHash; }
    public String getTenantId() { return tenantId; }
    public String getJobId() { return jobId; }
    public Instant getCreatedAt() { return createdAt; }

    public long getHitCount() { return hitCount; }
}

