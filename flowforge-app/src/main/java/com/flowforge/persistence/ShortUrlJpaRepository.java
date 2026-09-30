package com.flowforge.persistence;

import com.flowforge.core.domain.ShortUrl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlJpaRepository extends JpaRepository<ShortUrlEntity, Long> {
    Optional<ShortUrl> findByLongUrlHash(String longUrlHash);
    Optional<ShortUrl> findByShortCode(String shortCode);
    List<ShortUrlEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
