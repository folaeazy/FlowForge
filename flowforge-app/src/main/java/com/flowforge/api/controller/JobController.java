package com.flowforge.api.controller;

import com.flowforge.api.dto.CreateJobRequest;
import com.flowforge.api.dto.JobSubmitResponse;
import com.flowforge.core.domain.Job;
import com.flowforge.engine.JobQueueService;
import com.flowforge.filter.RateLimiterFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single real entry point into FlowForge's job pipeline.
 * Every other client is a real HTTP client - the simulation engine module
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobQueueService jobQueueService;

    public JobController(JobQueueService jobQueueService) {
        this.jobQueueService = jobQueueService;
    }

    @PostMapping
    public ResponseEntity<JobSubmitResponse> submit(@Valid @RequestBody CreateJobRequest request,
                                                    HttpServletRequest httpRequest) {
        Object attr = httpRequest.getAttribute(RateLimiterFilter.CLIENT_IDENTITY_ATTR);
        String identity = attr != null ? attr.toString() : httpRequest.getRemoteAddr();
        Job job = Job.builder()
                .tenantId(identity)
                .type(request.type())
                .payload(request.payload())
                .build();

        JobQueueService.SubmitResult result = jobQueueService.submit(job);

        return switch (result) {
            case ACCEPTED -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new JobSubmitResponse(job.getJobId(), "ACCEPTED"));
            case QUEUE_FULL -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new JobSubmitResponse(job.getJobId(), "QUEUE_FULL"));
        };


    }
}
