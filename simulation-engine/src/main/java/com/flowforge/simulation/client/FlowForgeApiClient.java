package com.flowforge.simulation.client;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


import java.util.Map;

/**
 * HTTP client for submitting jobs to FlowForge's /api/jobs endpoint.
 *
 * This is the piece that makes the simulator an external client,
 * not an internal Spring bean — it uses RestTemplate to make actual
 * HTTP calls across the network, the way a real customer would.
 */

@Component
public class FlowForgeApiClient {
    private static final Logger log = LoggerFactory.getLogger(FlowForgeApiClient.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public FlowForgeApiClient(SimulationProperties props) {
        this.restTemplate = createRestTemplate();
        this.baseUrl = props.getFlowforgeBaseUrl();
    }


    /**
     * Create a RestTemplate with reasonable timeouts for HTTP calls.
     */
    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        factory.setReadTimeout(java.time.Duration.ofSeconds(10));
        return new RestTemplate(new BufferingClientHttpRequestFactory(factory));

    }

    /**
     * Submit a single job to FlowForge.
     *
     * @return SubmitResult indicating what happened: ACCEPTED, RATE_LIMITED, or QUEUE_FULL
     */
    public SubmitResult submitJob(String tenantId, String type, Map<String, Object> payload, boolean simulateFailure) {
        Map<String, Object> request = Map.of(
                "tenantId", tenantId,
                "type", type,
                "payload", payload != null ? payload : Map.of("simulateFailure", simulateFailure)
        );

        try{
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/api/jobs",
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                return SubmitResult.ACCEPTED;
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return SubmitResult.RATE_LIMITED;
            } else if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                return SubmitResult.QUEUE_FULL;
            } else {
                log.warn("Unexpected status: {}", response.getStatusCode());
                return SubmitResult.ERROR;
            }
        } catch (RestClientException e) {
            log.error("Failed to submit job: {}", e.getMessage());
            return SubmitResult.ERROR;
        }
    }


    public enum SubmitResult {
        ACCEPTED, RATE_LIMITED, QUEUE_FULL, ERROR
    }
}
