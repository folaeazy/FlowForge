package com.flowforge.persistence;

import com.flowforge.core.domain.ShortUrl;
import com.flowforge.core.ports.ShortUrlStore;
import com.flowforge.exception.DuplicateShortCodeException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaShortUrlStore implements ShortUrlStore {

    private final ShortUrlJpaRepository repository;

    private static final String SHORT_CODE_INDEX = "idx_short_urls_short_code";

    public JpaShortUrlStore(ShortUrlJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ShortUrl> findByLongUrlHash(String longUrlHash) {
        return repository.findByLongUrlHash(longUrlHash);
    }

    @Override
    public Optional<ShortUrl> findByShortCode(String shortCode) {
        return repository.findByShortCode(shortCode);
    }

    @Override
    @Transactional
    public ShortUrl save(ShortUrl s) {

        try {
            repository.insertIfUrlIsAbsent(s.shortCode(), s.longUrl(), s.longUrlHash(),
                    s.tenantId(), s.jobId());


        } catch (DataIntegrityViolationException e) {
            // Same-URL races are absorbed by ON CONFLICT, so the only unique index left to violate is
            // short_code: a different URL owns this code. Match the root cause (just the PostgreSQL
            // error), never the wrapper, whose message also contains the INSERT text.
            String root = e.getMostSpecificCause().getMessage();
            if (root != null && root.contains(SHORT_CODE_INDEX)) {
                throw new DuplicateShortCodeException(s.shortCode());
            }
            throw e;
        }
        return repository.findByLongUrlHash(s.longUrlHash())
                .orElseThrow(()-> new IllegalStateException("Row missing right after insert"));
    }

    @Override
    public List<ShortUrl> findRecentByTenant(String tenantId, int page, int size) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<ShortUrl> findRecent(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page,size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void addHits(String shortCode, long delta) {
        repository.addHits(shortCode, delta);
    }


    private ShortUrl toDomain(ShortUrlEntity e) {
        return new ShortUrl(e.getId(), e.getShortCode(), e.getLongUrl(), e.getLongUrlHash(),
                e.getTenantId(), e.getJobId(),e.getHitCount(), e.getCreatedAt());
    }
}
