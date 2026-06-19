package com.flowforge.engine;

import com.flowforge.core.ports.IdempotencyStore;
import com.flowforge.core.ports.MetricsStore;
import com.flowforge.events.JobEventsPublisher;


/**
 * JobWorker already needs workerId, queue, processor, retryPolicy, dlq,
 * and retryScheduler. Adding idempotencyStore, metricsStore, and
 * eventPublisher individually pushes the constructor to 9 parameters —
 * past the point where the list communicates anything useful at a glance.
 * These three are always supplied together, by the same caller
 * (WorkerPool), so bundling them as one unit keeps the constructor
 * readable without reaching for a builder.
 */
public record JobWorkerSupport (
    IdempotencyStore idempotencyStore,
    MetricsStore metricsStore,
    JobEventsPublisher eventsPublisher
){ }
