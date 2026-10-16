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
import org.springframework.web.client.HttpStatusCodeException;
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Forwarded-For", request.identity().ip());   // the gateway reads identity from here

        Map<String, Object> body = Map.of(
                "type", "URL_SHORTEN",
                "payload", Map.of("longUrl" , request.longUrl())
        );

        try {

            log.debug("Submitting job: ip={}, longUrl={}", request.identity().ip(), request.longUrl());

            ResponseEntity<Void> response = restTemplate.postForEntity(
                    baseUrl + "/api/jobs",
                    new HttpEntity<>(body, headers),
                    Void.class
            );

            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                return SubmitResult.ACCEPTED;
            }
            log.warn("Unexpected status {} for ip={}", response.getStatusCode(), request.identity().ip());
            return SubmitResult.ERROR;

        } catch (HttpStatusCodeException e) {
            return switch (e.getStatusCode().value()) {
                case 429 -> SubmitResult.RATE_LIMITED;
                case 503 -> SubmitResult.QUEUE_FULL;
                default -> {
                    log.warn("Unexpected status {} for ip={}", e.getStatusCode(), request.identity().ip());
                    yield SubmitResult.ERROR;
                }
            };
        } catch (RestClientException e) {
            log.error("Failed to submit job: {}", e.getMessage()); //TODO : watch
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
