package com.flowforge.persistence;

import com.flowforge.core.domain.ShortUrl;
import com.flowforge.core.ports.ShortUrlStore;
import com.flowforge.exception.DuplicateShortCodeException;
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
    public ShortUrl save(ShortUrl shortUrl) {
        ShortUrlEntity entity = new ShortUrlEntity(
                shortUrl.shortCode(), shortUrl.longUrl(), shortUrl.longUrlHash(),
                shortUrl.tenantId(), shortUrl.jobId(), shortUrl.createdAt()
        );
        try {
            ShortUrlEntity saved = repository.save(entity);
            repository.flush(); // force the constraint check now, not at some later flush
            return toDomain(saved);

        } catch (DataIntegrityViolationException e) {
            if (isConstraintViolation(e, "short_urls_short_code_key")
                    || isConstraintViolation(e, "short_code")) {
                throw new DuplicateShortCodeException(shortUrl.shortCode());
            }
            // long_url_hash collision — let it propagate. UrlShortenerJobProcessor
            // catches this generically and re-queries findByLongUrlHash to adopt
            // the winner's code instead of treating it as a real failure.
            throw e;
        }
    }

    @Override
    public List<ShortUrl> findRecentByTenant(String tenantId, int page, int size) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }




    /**
     * Checks the actual DB constraint name rather than string-matching the
     * exception message, which is driver/version-fragile.
     */
    private boolean isConstraintViolation(DataIntegrityViolationException e, String needle) {
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException cve) {
            String constraintName = cve.getConstraintViolations().toString();
            return constraintName != null && constraintName.toLowerCase().contains(needle.toLowerCase());
        }
        return e.getMessage() != null && e.getMessage().toLowerCase().contains(needle.toLowerCase());
    }

    private ShortUrl toDomain(ShortUrlEntity e) {
        return new ShortUrl(e.getId(), e.getShortCode(), e.getLongUrl(), e.getLongUrlHash(),
                e.getTenantId(), e.getJobId(), e.getCreatedAt());
    }
}
