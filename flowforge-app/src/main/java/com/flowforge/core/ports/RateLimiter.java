package com.flowforge.core.ports;


import com.flowforge.api.dto.Tenant;
import com.flowforge.core.domain.RateLimitResult;

/**
 * Ports define what rate limiter does not how
 * The engine depends on the interface
 */
public interface RateLimiter {

    /**
     * Attempts to acquire 1 token for a given tenant
     */
    RateLimitResult tryAcquire(String tenantId);

    /**
     * Register a tenant with custom limits (e.g. paid tiers).
     * Tenants not registered use system defaults.
     */
    void registerTenant(String tenantId, long capacity, long ratePerSecond);

    /**
     * How many tokens does this tenant have available right now?
     * Used by the dashboard for rate limit bar visualization.
     */
    long availableTokens(String tenantId);

    /**
     * Returns this tenant's configured limits. Falls back to system
     */
    Tenant getTenantConfig(String tenantId);


}
