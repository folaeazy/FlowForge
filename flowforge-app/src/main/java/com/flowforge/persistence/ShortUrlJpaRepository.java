package com.flowforge.persistence;

import com.flowforge.core.domain.ShortUrl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlJpaRepository extends JpaRepository<ShortUrlEntity, Long> {

    Optional<ShortUrl> findByLongUrlHash(String longUrlHash);

    Optional<ShortUrl> findByShortCode(String shortCode);

    List<ShortUrlEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<ShortUrlEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Atomic "insert unless this URL is already stored". Returns 1 if inserted, 0 if the URL already existed. */
    @Modifying
    @Query(
            value = """
        INSERT INTO short_urls (short_code, long_url, long_url_hash, tenant_id, job_id)
        VALUES (:shortCode, :longUrl, :longUrlHash, :tenantId, :jobId)
        ON CONFLICT (long_url_hash) DO NOTHING
        """, nativeQuery = true
    )
    int insertIfUrlIsAbsent(
            @Param("shortCode") String shortCode, @Param("longUrl") String longUrl,
            @Param("longUrlHash") String longUrlHash, @Param("tenantId") String tenantId,
            @Param("jobId") String jobId
    );


    /**
     * Batch update of clicks from cache system
     */
    @Modifying
    @Query("UPDATE ShortUrlEntity e SET e.hitCount = e.hitCount + :delta WHERE e.shortCode = :shortCode")
    int addHits(@Param("shortCode") String shortCode, @Param("delta") long delta);
}
