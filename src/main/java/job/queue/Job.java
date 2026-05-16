package job.queue;

import java.util.Random;

/**
 * Job To be done with job id and tenant id
 * Retry mechanism implemented
 */
public class Job {
    String idempontencyKey;
    String id;
    String tenantId;
    int maxRetries;
    int retryCount;
    volatile boolean completed = false;

    public Job(String id, String tenantId, int maxRetries, String idmpKey) {
        this.id = id;
        this.tenantId = tenantId;
        this.maxRetries = maxRetries;
        this.retryCount = 0;
        this.idempontencyKey = idmpKey;
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void incrementRetry() {
        retryCount++;
    }

    public long nextDelayMillis() {
        return (long) Math.pow(2, retryCount) * 500;
    }



}
