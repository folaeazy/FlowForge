import job.queue.Job;
import job.queue.JobQueueService;
import job.queue.Worker;
import rate.limiter.TenantRateLimiterService;

import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        var tenantRateLimiter = new TenantRateLimiterService(15,20);
        var service = new JobQueueService(10, tenantRateLimiter); // maximum of 10 queue size

        var executor = Executors.newFixedThreadPool(3); // worker thread

        for (int i = 1; i <= 3; i++) {
            executor.submit(new Worker(service.getQueue(), "Worker " + i));
        }

        for (int i = 1; i <= 20; i++) { // 20 jobs
            service.submit(new Job("Job-" + i, "tenantA"));
        }
    }
}
