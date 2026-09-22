package com.flowforge.simulation.identity;

/**
 * Represents a simulated user identity (IP address) with its category.
 */
public record Identity (
        String ip,
        IdentityPool.Category category
) {
    public Identity {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("IP cannot be blank");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
    }
}
