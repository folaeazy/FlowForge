package com.flowforge.simulation.request;

import com.flowforge.simulation.identity.Identity;
import com.flowforge.simulation.identity.IdentityPool;

import java.time.Instant;

/**
 * Represents a single simulated request to shorten a URL.
 */
public record SimulationRequest(
        Identity identity,
        String longUrl,
        Instant timestamp
) {

    public SimulationRequest {
        if (identity == null) {
            throw new IllegalArgumentException("identity cannot be null");
        }
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("longUrl cannot be null or blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cannot be null");
        }
    }

    /**
     * Factory method for creating a request with the current timestamp.
     */
    public static SimulationRequest create(Identity identity, String longUrl) {
        return new SimulationRequest(identity, longUrl, Instant.now());
    }

    /**
     * Returns the IP address (tenantId for rate limiting).
     */
    public String getTenantId() {
        return identity.ip();
    }

    /**
     * Returns the category of the identity making this request.
     */
    public IdentityPool.Category getCategory() {
        return identity.category();
    }

    @Override
    public String toString() {
        return "SimulationRequest{" +
                "ip=" + identity.ip() +
                ", category=" + identity.category() +
                ", longUrl=" + longUrl +
                ", timestamp=" + timestamp +
                '}';
    }

}
