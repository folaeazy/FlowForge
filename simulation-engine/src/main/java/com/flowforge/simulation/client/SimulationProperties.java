package com.flowforge.simulation.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized config for the simulator — pulled from application.yml.
 * Lets operators point the simulator at different FlowForge instances
 * without code changes.
 */
@Component
@ConfigurationProperties(prefix = "flowforge")
public class SimulationProperties {
    private String baseUrl = "http://localhost:8080";

    public String getFlowforgeBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
