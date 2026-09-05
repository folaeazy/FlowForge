package com.flowforge.engine;

import com.flowforge.core.domain.Job;
import com.flowforge.core.domain.RetryPolicy;
import com.flowforge.core.ports.JobProcessor;
import com.flowforge.events.JobEvent;
import com.flowforge.events.JobEventsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class JobWorkerTest {

    private BlockingQueue<Job> queue;
    private DeadLetterQueue dlq;
    private ScheduledExecutorService retryScheduler;
    private FakeIdempotencyStore idempotencyStore;
    private FakeMetricsStore metricsStore;
    private List<JobEvent> capturedEvents;
    private JobWorkerSupport support;



    @BeforeEach
    void setUp() {
        queue = new LinkedBlockingQueue<>();
        dlq = new DeadLetterQueue(10);
        retryScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("test-retry"));
        idempotencyStore = new FakeIdempotencyStore();
        metricsStore = new FakeMetricsStore();
        capturedEvents = new CopyOnWriteArrayList<>();

        // ApplicationEventPublisher is a single-method interface — a lambda
        // capturing events into a list is all the "publisher" we need here.
        JobEventsPublisher eventPublisher = new JobEventsPublisher(event -> capturedEvents.add((JobEvent) event));
        support = new JobWorkerSupport(idempotencyStore, metricsStore, eventPublisher);
    }

    private JobWorker workerWith(JobProcessor processor) {
        return new JobWorker("test-worker", queue, processor, RetryPolicy.defaultPolicy(), dlq, retryScheduler, support);
    }

    @Test
    @DisplayName("Successful job is marked processed and publishes JobCompleted")
    void shouldProcessSuccessfully() {
        JobWorker worker = workerWith(job -> {});
        Job job = Job.builder().tenantId("tenant-A").type("TEST").build();

        worker.processJob(job);

        assertThat(idempotencyStore.isProcessed(job.getJobId())).isTrue();
        assertThat(metricsStore.getProcessed("tenant-A")).isEqualTo(1);
        assertThat(capturedEvents).anyMatch(e -> e instanceof JobEvent.JobCompleted);
    }

}
