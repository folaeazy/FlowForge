package com.flowforge.engine;

import com.flowforge.core.domain.Job;
import com.flowforge.core.domain.RetryPolicy;
import com.flowforge.core.ports.JobProcessor;
import com.flowforge.events.JobEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.concurrent.*;

/**
 * Job Consumer or Subscriber
 */
public class JobWorker implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private static final long POLL_TIMEOUT_MS = 500;

    private final String workerId;
    private final BlockingQueue<Job> queue;
    private final JobProcessor processor;
    private final RetryPolicy retryPolicy;
    private final DeadLetterQueue deadLetterQueue;
    private final ScheduledExecutorService retryScheduler;
    private final JobWorkerSupport support;

    private volatile boolean running = true;

    public JobWorker(String workerId, BlockingQueue<Job> queue, JobProcessor processor, RetryPolicy retryPolicy, DeadLetterQueue deadLetterQueue, ScheduledExecutorService retryScheduler, JobWorkerSupport support) {
        this.workerId = workerId;
        this.queue = queue;
        this.processor = processor;
        this.retryPolicy = retryPolicy;
        this.deadLetterQueue = deadLetterQueue;
        this.retryScheduler = retryScheduler;
        this.support = support;
    }


    @Override
    public void run() {
        log.info("[{}] Started", workerId);
        while (running) {
            try {

                Job job = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if(job == null) continue;
                processJob(job);
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("[{}] Interrupted — shutting down", workerId);
                break;

            }
            log.info("[{}] Stopped", workerId);
        }
    }

    /**
     * Package-private (not private) deliberately — lets JobWorkerTest call
     * this directly instead of driving the full run() poll loop, which
     * would make tests slower and timing-dependent for no benefit.
     *
     * MDC SCOPING — IMPORTANT: worker threads are long-lived, processing
     * many jobs across their lifetime. MDC values are thread-local and
     * persist until explicitly cleared. Without the finally block, job-1's
     * tenantId would still tag every log line for job-2 on the same
     * thread — an easy, real production bug. MDC.clear() guarantees a
     * clean slate before this thread picks up its next job.
     */
    void processJob(Job job) {
        Job processing = job.markProcessing();

        MDC.put("tenantId", processing.getTenantId());
        MDC.put("jobId", processing.getJobId());
        MDC.put("attempt", String.valueOf(processing.getAttemptCount()));

        try {
            if(support.idempotencyStore().isProcessed(processing.getJobId())) {
                log.info("[{}] Skipping already-processed job", workerId);
                support.eventsPublisher().publish(
                        new JobEvent.JobSkippedDuplicate(processing.getJobId() , processing.getTenantId(), Instant.now()));
                return;
            }
            log.info("[{}] Processing", workerId);
            support.eventsPublisher().publish(new JobEvent.JobProcessingStarted(
                    processing.getJobId(), processing.getTenantId(), workerId,
                    processing.getAttemptCount(), Instant.now()));

            processor.process(processing);

            // Mark processed ONLY after success.
            support.idempotencyStore().markProcessed(processing.getJobId(), 86_400);
            support.metricsStore().incrementProcessed(processing.getTenantId());

            log.info("[{}] Completed", workerId);
            support.eventsPublisher().publish(new JobEvent.JobCompleted(
                    processing.getJobId(), processing.getTenantId(), workerId, Instant.now()
            ));

        }catch (Exception e) {
            handleFailure(processing, e);
        }finally {
            MDC.clear();
        }

    }


    private void handleFailure(Job job, Exception cause) {
        log.warn("[{}] Failed: {}", workerId, cause.getMessage());
        support.metricsStore().incrementFailed(job.getTenantId()); //update metrics

        if(job.hasRetriesLeft()){
            Job failed = job.markFailed();
            long delayMs = retryPolicy.delayMs(job.getAttemptCount());
            support.metricsStore().incrementRetried(job.getTenantId());
            support.eventsPublisher().publish(new JobEvent.JobRetryScheduled(
                    job.getJobId(), job.getTenantId(), delayMs, job.getAttemptCount() + 1, Instant.now()
            ));
            log.info("[{}] Scheduling retry in {}ms", workerId, delayMs);
            retryScheduler.schedule(() -> reEnqueue(failed),delayMs, TimeUnit.MILLISECONDS);

        }else {
            Job dead = job.markDead();

            deadLetterQueue.park(dead);
            support.eventsPublisher().publish(new JobEvent.JobDead(
                    job.getJobId(), job.getTenantId(), cause.getMessage(), Instant.now()
            ));
            log.error("[{}] Retries exhausted — sent to DLQ", workerId);
        }
    }

    private void reEnqueue(Job job) {
        boolean enqueued = queue.offer(job);
        if (!enqueued) {
            log.error("[RETRY] Queue full — dropping to DLQ: {}", job.getJobId());
            deadLetterQueue.park(job.markDead());
        }
    }
    public void stop() {
        this.running = false;
    }
}













//    private final BlockingQueue<Job> queue;
//    private final String name;
//    private final BlockingQueue<Job> deadLetterQueue = new LinkedBlockingDeque<>();
//    private final Set<String> completedJobs = ConcurrentHashMap.newKeySet();
//
//    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//
//    public JobWorker(BlockingQueue<Job> queue, String name) {
//        this.queue = queue;
//        this.name = name;
//    }
//
//    private boolean process(Job job) {
//        return Math.random() > 0.3; // 70% success, 30% failure
//    }
//
//    @Override
//    public void run() {
//        while (true) {
//            try {
//
//                Job job = queue.take();
//                if(completedJobs.contains(job.id)) {
//                    System.out.println("Duplicate skipped: " + job.id);
//                    continue;
//                }
//                System.out.println(
//                        name + " processing " + job.id +
//                                " | instance=" + System.identityHashCode(job)
//                );
//                Thread.sleep(200); // simulate real work
//
//                boolean success = process(job);
//                if(success) {
//                    System.out.println(name + " DONE " + job.id);
//                    job.completed = true;
//                    completedJobs.add(job.id);
//                }else {
//                    System.out.println(name + " FAILED " + job.id);
//
//                    if(job.canRetry()){
//                        job.incrementRetry();
//
//                        long delay = job.nextDelayMillis();
//
//                        System.out.println(name + " RETRYING " + job.id + " in " + delay + "ms" +
//                                " (attempt " + job.retryCount + ")");
//                        scheduler.schedule(() -> {
//                            if(job.completed) {
//                                System.out.println("Skipping retry job " + job.id + " already completed");
//                                return;
//                            }
//                            System.out.println("Re-enqueueing " + job.id);
//                            queue.offer(job);
//                        }, delay, TimeUnit.MILLISECONDS);
//
//                    } else {
//                    System.out.println(name + " moving to DLQ " + job.id +
//                            " (max retries reached)");
//                    deadLetterQueue.offer(job);
//                   }
//                }
//
//            }catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//
//
//    }
//
//    public void printDLQ() {
//        System.out.println("---- DLQ CONTENT ----");
//        deadLetterQueue.forEach(job ->
//                System.out.println(job.id + " (retries: " + job.retryCount + ")")
//        );
//    }

