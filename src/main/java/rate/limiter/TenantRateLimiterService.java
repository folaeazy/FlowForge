package rate.limiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per tenant rate-limiter
 * Each Tenant gets it's isolated token bucket
 */
public class TenantRateLimiterService {

    private final ConcurrentHashMap<String, TokenBucket> tenantBuckets;
    private final long defaultCapacity;
    private final long defaultTokenPerSecond;


    public TenantRateLimiterService(long defaultCapacity, long defaultTokenPerSecond) {
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
    public void registerTenant(String tenantId, long capacity, long ratePerSecond) {
        tenantBuckets.compute(
                tenantId, (id, existing) ->
                        new TokenBucket(capacity, ratePerSecond)
        );

    }


    /**
     * Active tenant count
     */
    public int activeTenantCount() {
        return tenantBuckets.size();
    }

    public long availableTokens(String tenantId) {
        TokenBucket bucket = tenantBuckets.get(tenantId);

        return bucket.availableTokens();
    }





}
