package rate.limiter;

public record RateLimitResult(
        boolean allowed,
        String tenantId,
        double availableTokens,
        String reason
) {

    public static RateLimitResult allowed(String tenantId, double tokens) {
        return new RateLimitResult(true, tenantId, tokens, null);
    }

    public static RateLimitResult rejected(String tenantId, double tokens){
        return new RateLimitResult(false, tenantId, tokens, "rate limit exceeded");
    }
}
