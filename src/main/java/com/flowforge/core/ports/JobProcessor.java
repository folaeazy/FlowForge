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
    void process(Job job) throws Exception;
}
