package com.flowforge.core.ports;


import com.flowforge.core.domain.Job;

/**
 * Port — the actual business logic that processes a job.
 *  *
 *  * FlowForge doesn't know what jobs DO — that's the application's concern.
 *  * This interface is injected into the engine, keeping the engine generic.
 *  *
 *  * @FunctionalInterface means implementations can be lambdas —
 */
@FunctionalInterface
public interface JobProcessor {

    /**
     * Process a job. Throw any exception to signal failure.
     * MUST be idempotent — the same job may arrive more than once on retry.
     */
    JobProcessor.Result process(Job job) throws Exception;

    /**
     * Returns the job type this processor handles.
     * Example: "URL_SHORTEN", "TEST_JOB", "EMAIL_SEND", etc.
     */
    String getJobType();



    record Result(
            boolean success,
            String output,           // Job-specific result (e.g., short code)
            String failureReason     // If success=false, why it failed
    ) {}
}
