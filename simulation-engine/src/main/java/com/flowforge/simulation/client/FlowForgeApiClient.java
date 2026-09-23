package com.flowforge.simulation.client;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.simulation.request.SimulationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


import java.util.HashMap;
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
    private final ObjectMapper  objectMapper;

    public FlowForgeApiClient(SimulationProperties props, ObjectMapper objectMapper) {
        this.restTemplate = createRestTemplate();
        this.baseUrl = props.getFlowforgeBaseUrl();
        this.objectMapper = objectMapper;
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
     * @param  request - containing IP and LongUrl
     * @return SubmitResult indicating what happened: ACCEPTED, RATE_LIMITED, or QUEUE_FULL
     */
    public SubmitResult submitJob(SimulationRequest request)  {

        try {
            String ip = request.identity().ip();
            String longUrl = request.longUrl();

            // Job Payload
            Map<String, Object> jobPayload = new HashMap<>();
            jobPayload.put("longUrl", longUrl);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("tenantId", ip);  // IP is used as tenantId for rate limiting
            requestBody.put("type", "URL_SHORTEN");
            requestBody.put("payload", jobPayload);

            // HTTP Header
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-Id", ip);  // Rate limiter intercepts this
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Serialize to JSON
            String body = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> httpRequest = new HttpEntity<>(body, headers);

            log.debug("Submitting job: ip={}, longUrl={}", ip, longUrl);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/api/jobs",
                    httpRequest,
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
            log.error("Failed to submit job: {}", e.getMessage()); //TODO : watch
            return SubmitResult.ERROR;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request body", e);
            return SubmitResult.ERROR;
        } catch (Exception e) {
            log.error("Unexpected error submitting job", e);
            return SubmitResult.ERROR;
        }
    }


    public enum SubmitResult {
        ACCEPTED, RATE_LIMITED, QUEUE_FULL, ERROR
    }
}
