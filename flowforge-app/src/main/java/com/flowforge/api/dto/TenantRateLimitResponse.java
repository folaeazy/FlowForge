package com.flowforge.api.dto;

public record TenantRateLimitResponse(
        String tenantId,
        long capacity,
        long availableTokens,
        double usagePercent
) { }
