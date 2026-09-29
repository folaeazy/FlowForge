package com.flowforge.url_short;

import com.flowforge.core.domain.Job;
import com.flowforge.core.domain.ShortUrl;
import com.flowforge.core.ports.JobProcessor;
import com.flowforge.core.ports.ShortUrlStore;
import com.flowforge.exception.DuplicateShortCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class UrlShortenerJobProcessor implements JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerJobProcessor.class);
    private static final String BASE62_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final int MAX_COLLISION_RETRIES = 10;

    private final ShortUrlStore shortUrlStore;

    public UrlShortenerJobProcessor(ShortUrlStore shortUrlStore) {
        this.shortUrlStore = shortUrlStore;
    }



    @Override
    public Result process(Job job)  {
        String longUrl = extractLongUrl(job);

        // validate Url - real authentic failure
        if(!isValidUrl(longUrl)) {
            String reason = describeInvalidReason(longUrl);
            log.warn("Invalid URL rejected {} : ({}", longUrl, reason);
            return new Result(false, null, "Invalid URL" + reason);
        }

        // create long url HASH
        String longUrlHash = SHA256Hash(longUrl);

        // Global Idempotency - same url - any tenant - same short code
        Optional<ShortUrl> existing = shortUrlStore.findByLongUrlHash(longUrlHash);
        if(existing.isPresent()) {
            log.info("URL already shortened, reusing code: {}", existing.get().shortCode());
            return new Result(true,existing.get().shortCode(), null );
        }

        //  Generate + persist, with real collision retry
        for(int attempt = 0; attempt <= MAX_COLLISION_RETRIES; attempt++) {
            String candidateInput = attempt == 0 ? longUrl : longUrl + "#" + attempt;
            String shortCode = base62Encode(sha256Bytes(candidateInput), SHORT_CODE_LENGTH);

            ShortUrl toSave =  new ShortUrl(
                    null, shortCode, longUrl,
                    longUrlHash, job.getTenantId(), job.getJobId(), Instant.now()
            );
            try {
                ShortUrl saved = shortUrlStore.save(toSave);
                log.info("Shortened: {} -> {}", saved.shortCode(), longUrl);
                return new Result(true, saved.shortCode(), null);

            }catch (DuplicateShortCodeException ex) {
                log.debug("short_code collision on attempt {} ({}), retrying with salted hash", attempt, shortCode);
                // loop continues with a different candidateInput
            }catch (Exception e){
                // Most likely long_url_hash race — a concurrent job for the SAME
                // url won first. That's success for this job, not failure.
                Optional<ShortUrl> winner = shortUrlStore.findByLongUrlHash(longUrlHash);
                if (winner.isPresent()) {
                    log.info("Lost idempotency race, adopting winner's code: {}", winner.get().shortCode());
                    return new Result(true, winner.get().shortCode(), null);
                }
                log.error("Unexpected persistence error shortening URL", e);
                return new Result(false, null, "Persistence error: " + e.getMessage());
            }

        }

        // Exhausted retries — real failure, feeds DLQ correctly now that
        // JobWorker's else-branch routes through handleFailure().
        log.error("Exhausted {} collision retries for: {}", MAX_COLLISION_RETRIES, longUrl);
        return new Result(false, null, "Short code collision retries exhausted");


    }

    private String base62Encode(byte[] bytes, int length) {
        BigInteger num = new BigInteger(1, bytes);
        BigInteger base = BigInteger.valueOf(62);
        StringBuilder sb = new StringBuilder();

        while (num.compareTo(BigInteger.ZERO) > 0 && sb.length() < length) {
            BigInteger[] divmod = num.divideAndRemainder(base);
            sb.append(BASE62_ALPHABET.charAt(divmod[1].intValue()));
            num = divmod[0];
        }
        while (sb.length() < length) sb.append(BASE62_ALPHABET.charAt(0));
        return sb.reverse().toString();
    }


    private String SHA256Hash(String input) {
        byte[] hash = sha256Bytes(input);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private byte[] sha256Bytes(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String describeInvalidReason(String url) {
        if (url == null || url.isBlank()) return "empty or blank";
        if (url.chars().anyMatch(Character::isWhitespace)) return "contains whitespace";
        try {
            URI uri = new URI(url);
            return uri.getScheme() == null ? "missing protocol" : "malformed structure";
        } catch (URISyntaxException e) {
            return "invalid characters";
        }
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        if (url.chars().anyMatch(Character::isWhitespace)) return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String extractLongUrl(Job job) {
        Object payload = job.getPayload();
        if (payload instanceof Map<?, ?> map) {
            Object longUrl = map.get("longUrl");
            return longUrl != null ? longUrl.toString() : null;
        }
        return null;
    }

    @Override
    public String getJobType() {
        return "URL_SHORTEN";
    }
}
