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
    /** Request attribute the controller reads to know who made the request. */
    public static final String CLIENT_IDENTITY_ATTR = "flowforge.clientIdentity";

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

        String identity = resolveClientIdentity(httpRequest);
        httpRequest.setAttribute(CLIENT_IDENTITY_ATTR, identity);


        // Check rate limit (this is the gateway check)
         RateLimitResult result = rateLimiter.tryAcquire(identity);

        if (!result.allowed()) {
            // Rate limited — return 429 immediately, never reach controller
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("Rate limit exceeded..");
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);

    }


    /**
     * First X-Forwarded-For value if present (the simulation plays the role of
     * a proxy), otherwise the socket's remote address.
     */
    private String resolveClientIdentity(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isSubmissionPath(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/api/jobs");
    }
}
