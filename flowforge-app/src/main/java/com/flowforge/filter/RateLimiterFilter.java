package com.flowforge.filter;

import com.flowforge.core.domain.RateLimitResult;
import com.flowforge.core.ports.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Gateway filter that rate limits all incoming requests before they reach controllers.
 * Extracts tenantId from request header and checks against the rate limiter.
 * Returns 429 Too Many Requests if tenant is rate limited.
 */
@Component
public class RateLimiterFilter implements Filter {
    private final RateLimiter rateLimiter;

    public RateLimiterFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        // Only gate job SUBMISSION — not reads (redirects, URL lookups, dashboard queries)
        if (!isSubmissionPath(httpRequest)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Extract tenantId from request header
        String tenantId = httpRequest.getHeader("X-Tenant-Id");

        // If no header, try to extract from request body or use default
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        // Check rate limit (this is the gateway check)
         RateLimitResult result = rateLimiter.tryAcquire(tenantId);

        if (!result.allowed()) {
            // Rate limited — return 429 immediately, never reach controller
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("Rate limit exceeded");
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);

    }


    private boolean isSubmissionPath(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/api/jobs");
    }
}
