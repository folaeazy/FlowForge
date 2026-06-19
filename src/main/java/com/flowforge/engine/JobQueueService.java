package com.flowforge.engine;

import com.flowforge.core.domain.Job;
import com.flowforge.core.domain.RateLimitResult;
import com.flowforge.core.ports.MetricsStore;
import com.flowforge.core.ports.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rate.limiter.TenantRateLimiterService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 * Job publisher or producer
 * Using Blocking queue
 */

@Service
public class JobQueueService {
    private static final Logger log = LoggerFactory.getLogger(JobQueueService.class);

    public enum SubmitResult { ACCEPTED, RATE_LIMITED, QUEUE_FULL }

    private final BlockingQueue<Job> jobQueue;
    private final RateLimiter rateLimiter;
    private final MetricsStore metricsStore;

    public JobQueueService(BlockingQueue<Job> jobQueue, RateLimiter rateLimiter, MetricsStore metricsStore) {
        this.jobQueue = jobQueue;
        this.rateLimiter = rateLimiter;
        this.metricsStore = metricsStore;
    }

    public SubmitResult submit(Job job) {
        RateLimitResult limitResult = rateLimiter.tryAcquire(job.getTenantId());
        if (!limitResult.allowed()) {
            log.debug("[Submit] Rate limited tenant={}", job.getTenantId());
            return SubmitResult.RATE_LIMITED;
        }

        boolean enqueued = jobQueue.offer(job);
        metricsStore.recordQueueSize(jobQueue.size());

        if (!enqueued) {
            log.warn("[Submit] Queue full — rejected jobId={}", job.getJobId());
            return SubmitResult.QUEUE_FULL;
        }

        log.info("[Submit] Accepted jobId={} tenant={}", job.getJobId(), job.getTenantId());
        return SubmitResult.ACCEPTED;
    }

    public int queueSize() {
        return jobQueue.size();
    }



}












//    private final BlockingQueue<Job> queue;
//    private final TenantRateLimiterService tenantRateLimiterService;
//
//    public JobQueueService(int capacity,  TenantRateLimiterService tenantRateLimiterService) {
//        this.queue = new LinkedBlockingDeque<>(capacity);
//        this.tenantRateLimiterService = tenantRateLimiterService;
//    }
//
//    public boolean submit(Job job) {
//        if(!tenantRateLimiterService.tryAcquire(job.tenantId)){
//            System.out.println(job.id + " rejected (rate limit) ");
//            return false;
//        }
//
//        // add to queue
//        boolean offered = queue.offer(job);
//        if(!offered) {
//            System.out.println(job.id + " rejected (queue full) ");
//            return false;
//        }
//
//        System.out.println(job.id + " accepted" );
//        return true;
//
//    }
//
//
//    public BlockingQueue<Job> getQueue() {
//        return queue;
//    }

