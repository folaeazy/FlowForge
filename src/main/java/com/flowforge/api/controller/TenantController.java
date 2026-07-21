package com.flowforge.api.controller;

import com.flowforge.api.dto.Tenant;
import com.flowforge.api.dto.TenantRateLimitResponse;
import com.flowforge.core.ports.RateLimiter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/tenants")
public class TenantController {

    private final RateLimiter rateLimiter;

    public TenantController(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/{tenantId}/limit")
    public TenantRateLimitResponse getLimit(@PathVariable String tenantId) {
        long available = rateLimiter.availableTokens(tenantId);
        Tenant config = rateLimiter.getTenantConfig(tenantId);
        long capacity = config.capacity();
        double usagePercentage = capacity > 0
                ? Math.max(0, Math.min(100, ((double) (capacity - available) / capacity ) * 100))
                : 0;
        return new TenantRateLimitResponse(tenantId, capacity, available, usagePercentage);
    }

}
