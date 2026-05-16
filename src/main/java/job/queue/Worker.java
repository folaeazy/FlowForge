package job.queue;

import service.IdempotencyService;

import java.util.Set;
import java.util.concurrent.*;

/**
 * Job Consumer or Subscriber
 */
public class Worker implements Runnable{

    private final BlockingQueue<Job> queue;
    private final String name;
    private final BlockingQueue<Job> deadLetterQueue = new LinkedBlockingDeque<>();
    //

    private final IdempotencyService idempotencyService;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Worker(BlockingQueue<Job> queue, String name, IdempotencyService idempotencyService) {
        this.queue = queue;
        this.name = name;
        this.idempotencyService = idempotencyService;
    }

    private boolean process(Job job) {
        return Math.random() > 0.3; // 70% success, 30% failure
    }

    @Override
    public void run() {
        while (true) {
            try {

                Job job = queue.take();
                if(idempotencyService.isCompleted(job.idempontencyKey)) {
                    System.out.println("Duplicate skipped: (already completed) " + job.id);
                    continue;
                }
                System.out.println(
                        name + " processing " + job.id +
                                " | instance=" + System.identityHashCode(job)
                );
                if(!idempotencyService.tryAcquire(job.idempontencyKey)) {
                    System.out.println("Another is processing  " + job.id);
                    continue;
                }
                Thread.sleep(200); // simulate real work

                boolean success = process(job);
                if(success) {
                    System.out.println(name + " DONE " + job.id);
                    job.completed = true;
                    idempotencyService.markSuccess(job.idempontencyKey);
                }else {
                    System.out.println(name + " FAILED " + job.id);

                    if(job.canRetry()){
                        job.incrementRetry();

                        long delay = job.nextDelayMillis();

                        System.out.println(name + " RETRYING " + job.id + " in " + delay + "ms" +
                                " (attempt " + job.retryCount + ")");
                        scheduler.schedule(() -> {
                            if(idempotencyService.isCompleted(job.idempontencyKey)) {
                                System.out.println("Skipping retry job " + job.id );
                                return;
                            }
                            System.out.println("Re-enqueueing " + job.id);
                            if(!queue.offer(job)){
                                System.out.println("Retry dropped (queue full): "+ job.id);
                            }
                        }, delay, TimeUnit.MILLISECONDS);

                    } else {
                    System.out.println(name + " moving to DLQ " + job.id +
                            " (max retries reached)");
                    deadLetterQueue.offer(job);
                   }
                }

            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


    }

    public void printDLQ() {
        System.out.println("---- DLQ CONTENT ----");
        deadLetterQueue.forEach(job ->
                System.out.println(job.id + " (retries: " + job.retryCount + ")")
        );
    }
}
