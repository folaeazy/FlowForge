package com.flowforge.core.domain;

/**
 *
 * Value Object representing rate limit decision
 */
public record RateLimitResult(
        boolean allowed,
        String tenantId,
        long availableTokens,
        String reason
) {

    public static RateLimitResult allowed(String tenantId, long tokens) {
        return new RateLimitResult(true, tenantId, tokens, null);
    }

    public static RateLimitResult rejected(String tenantId, long tokens){
        return new RateLimitResult(false, tenantId, tokens, "Rate limit exceeded");
    }
}
