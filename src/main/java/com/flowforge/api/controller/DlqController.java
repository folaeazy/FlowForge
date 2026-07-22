package com.flowforge.api.controller;

import com.flowforge.api.dto.DlqEntryResponse;
import com.flowforge.core.domain.Job;
import com.flowforge.engine.DeadLetterQueue;
import com.flowforge.engine.JobQueueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Failed Jobs (DLQ) table - list, inspect and retry
 */
@RestController
@RequestMapping("/api/dlq")
public class DlqController {

    private final DeadLetterQueue dlq;
    private final JobQueueService jobQueueService;

    public DlqController(DeadLetterQueue dlq, JobQueueService jobQueueService) {
        this.dlq = dlq;
        this.jobQueueService = jobQueueService;
    }

    @GetMapping()
    public List<DlqEntryResponse> list(@RequestParam(required = false) String tenantId) {
        List<Job> jobs =  (tenantId != null) ? dlq.getByTenant(tenantId) : dlq.getAll();
        return jobs.stream().map(DlqEntryResponse::from).toList();
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<DlqEntryResponse> inspect(@PathVariable String jobId) {
        Optional<Job> found = dlq.getAll().stream()
                .filter(job -> job.getJobId().equals(jobId))
                .findFirst();
        return found
                .map(job -> ResponseEntity.ok(DlqEntryResponse.from(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * "Retry" action — manually re-submit a dead job through the normal
     * pipeline (rate limiter, queue, fresh attempt count via a NEW Job)
     */
    @PostMapping("/{jobId}/retry")
    public ResponseEntity<String> retry(@PathVariable String jobId) {
        Optional<Job> found = dlq.getAll().stream()
                .filter(j -> j.getJobId().equals(jobId))
                .findFirst();

        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Job original = found.get();
        Job resubmitted = Job.builder()
                .tenantId(original.getTenantId())
                .type(original.getType())
                .payload(original.getPayload())
                .maxRetries(original.getMaxRetries())
                .build();

        JobQueueService.SubmitResult result = jobQueueService.submit(resubmitted);

        if(result == JobQueueService.SubmitResult.ACCEPTED) {
            dlq.remove(jobId);
        }

        return switch (result) {
            case ACCEPTED -> ResponseEntity.ok("Resubmitted as jobId=" + resubmitted.getJobId());
            case RATE_LIMITED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Tenant rate limited");
            case QUEUE_FULL -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Queue full");

        };
    }
}
