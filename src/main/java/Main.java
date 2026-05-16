import job.queue.Job;
import job.queue.JobQueueService;
import job.queue.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import rate.limiter.TenantRateLimiterService;
import service.IdempotencyService;

import java.util.concurrent.Executors;

public class Main {

    StringRedisTemplate redisTemplate = new StringRedisTemplate();
    private final IdempotencyService idempotencyService = new IdempotencyService(redisTemplate);

    public static void main(String[] args) {
        var tenantRateLimiter = new TenantRateLimiterService(15,20);
        var service = new JobQueueService(10, tenantRateLimiter); // maximum of 10 queue size

        var executor = Executors.newFixedThreadPool(3); // worker thread

        for (int i = 1; i <= 3; i++) {
            executor.submit(new Worker(service.getQueue(), "Worker " + i, new IdempotencyService(new StringRedisTemplate())));
        }

        for (int i = 1; i <= 20; i++) { // 20 jobs
            service.submit(new Job("Job-" + i, "tenantA", 3, "IDMP_KEY-" + i));
        }


    }
}
