package com.flowforge.engine;

import com.flowforge.core.domain.Job;
import com.flowforge.core.domain.RetryPolicy;
import com.flowforge.core.ports.JobProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Owns the lifecycle of JobWorker threads.
 *
 * WHY @PostConstruct, NOT THE CONSTRUCTOR:
 * Constructors should establish object identity, not start background
 * threads — at construction time, Spring may not have finished wiring
 * every collaborator this bean needs. @PostConstruct guarantees all
 * @Autowired fields are set before this runs.
 *
 * WHY @PreDestroy:
 * Hooks into Spring's shutdown sequence, including Spring Boot's graceful
 * shutdown — which waits for in-flight work AND lets @PreDestroy methods
 * run before the JVM exits. Workers get a chance to finish their current
 * job rather than being killed mid-task.
 */

@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private final BlockingQueue<Job> jobQueue;
    private final JobProcessor processor;
    private final DeadLetterQueue dlq;
    private final ScheduledExecutorService retryScheduler;
    private final JobWorkerSupport support;
    private final RetryPolicy retryPolicy;
    private final int poolSize;
    private final int shutdownTimeoutSeconds;

    private final List<JobWorker> workers = new ArrayList<>();
    private ExecutorService workerExecutor;

    public WorkerPool(
            BlockingQueue<Job> jobQueue,
            JobProcessor processor,
            DeadLetterQueue dlq,
            ScheduledExecutorService retryScheduler,
            JobWorkerSupport support,
            @Value("${flowforge.workers.pool-size:4}") int poolSize,
            @Value("${flowforge.workers.shutdown-timeout-seconds:30}") int shutdownTimeoutSeconds,
            @Value("${flowforge.retry.base-delay-ms:1000}") long retryBaseDelayMs,
            @Value("${flowforge.retry.max-delay-ms:30000}") long retryMaxDelayMs
    ) {
        this.jobQueue = jobQueue;
        this.processor = processor;
        this.dlq = dlq;
        this.retryScheduler = retryScheduler;
        this.support = support;
        this.poolSize = poolSize;
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
        this.retryPolicy = new RetryPolicy(retryBaseDelayMs, retryMaxDelayMs);
    }
    @PostConstruct
    public void start() {
        workerExecutor = Executors.newFixedThreadPool(poolSize, new NamedThreadFactory("job-worker"));

        for (int i = 0; i < poolSize; i++) {
            JobWorker worker = new JobWorker(
                    "worker-" + i, jobQueue, processor, retryPolicy, dlq, retryScheduler, support
            );
            workers.add(worker);
            workerExecutor.submit(worker);
        }

        log.info("[WorkerPool] Started {} workers, retryPolicy={}", poolSize, retryPolicy);
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("[WorkerPool] Shutdown initiated...");

        workers.forEach(JobWorker::stop);
        workerExecutor.shutdown();

        boolean clean = workerExecutor.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS);
        if (!clean) {
            log.warn("[WorkerPool] Workers didn't finish in time — forcing shutdown");
            workerExecutor.shutdownNow();
        }

        retryScheduler.shutdown();
        retryScheduler.awaitTermination(5, TimeUnit.SECONDS);
        log.info("[WorkerPool] Shutdown complete");
    }

    public int activeWorkerCount() {
        return workers.size();
    }
}
