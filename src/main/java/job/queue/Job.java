package job.queue;

/**
 * Job To be done with id and tenant id
 */
public class Job {
    String id;
    String tenantId;

    public Job(String id, String tenantId) {
        this.id = id;
        this.tenantId = tenantId;
    }


}
