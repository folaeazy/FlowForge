package rate.limiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per tenant rate-limiter
 * Each Tenant gets it's isolated token bucket
 */
public class TenantRateLimiterService {

    private final ConcurrentHashMap<String, TokenBucket> tenantBuckets;
    private final double defaultCapacity;
    private final double defaultTokenPerSecond;


    public TenantRateLimiterService(double defaultCapacity, double defaultTokenPerSecond) {
        this.defaultCapacity = defaultCapacity;
        this.defaultTokenPerSecond = defaultTokenPerSecond;
        this.tenantBuckets = new ConcurrentHashMap<>();
    }

    /**
     * Check rate limit for a tenant , creates bucket on first access.
     */
    public boolean tryAcquire(String tenantId) {
        TokenBucket bucket = tenantBuckets.computeIfAbsent(
                tenantId,
                (id) -> new TokenBucket(defaultCapacity, defaultTokenPerSecond)
        );
        return bucket.tryAcquire();

    }

    /**
     * Create a custom rate limit for a tenant
     * Thread safe - With an atomic update of the tenant bucket
     */
    public void registerTenant(String tenantId, double capacity, double ratePerSecond) {
        tenantBuckets.compute(
                tenantId, (id, existing) ->
                        new TokenBucket(capacity, ratePerSecond)
        );

    }




}
