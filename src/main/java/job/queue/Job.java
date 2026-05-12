package job.queue;

/**
 * Job To be done with job id and tenant id
 * Retry mechanism implemented
 */
public class Job {
    String id;
    String tenantId;
    int maxRetries;
    int retryCount;

    public Job(String id, String tenantId, int maxRetries) {
        this.id = id;
        this.tenantId = tenantId;
        this.maxRetries = maxRetries;
        this.retryCount = 0;
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
