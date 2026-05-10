package job.queue;

import rate.limiter.TenantRateLimiterService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 * Job publisher or producer
 * Using Blocking queue
 */
public class JobQueueService {

    private final BlockingQueue<Job> queue;
    private final TenantRateLimiterService tenantRateLimiterService;

    public JobQueueService(int capacity,  TenantRateLimiterService tenantRateLimiterService) {
        this.queue = new LinkedBlockingDeque<>(capacity);
        this.tenantRateLimiterService = tenantRateLimiterService;
    }

    public boolean submit(Job job) {
        if(!tenantRateLimiterService.tryAcquire(job.tenantId)){
            System.out.println(job.id + " rejected (rate limit) ");
            return false;
        }

        // add to queue
        boolean offered = queue.offer(job);
        if(!offered) {
            System.out.println(job.id + " rejected (queue full) ");
            return false;
        }

        System.out.println(job.id + " accepted" );
        return true;

    }


    public BlockingQueue<Job> getQueue() {
        return queue;
    }
}
