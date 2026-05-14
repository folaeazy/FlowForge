package job.queue;

import java.util.Set;
import java.util.concurrent.*;

/**
 * Job Consumer or Subscriber
 */
public class Worker implements Runnable{

    private final BlockingQueue<Job> queue;
    private final String name;
    private final BlockingQueue<Job> deadLetterQueue = new LinkedBlockingDeque<>();
    private final Set<String> completedJobs = ConcurrentHashMap.newKeySet();

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Worker(BlockingQueue<Job> queue, String name) {
        this.queue = queue;
        this.name = name;
    }

    private boolean process(Job job) {
        return Math.random() > 0.3; // 70% success, 30% failure
    }

    @Override
    public void run() {
        while (true) {
            try {

                Job job = queue.take();
                if(completedJobs.contains(job.id)) {
                    System.out.println("Duplicate skipped: " + job.id);
                    continue;
                }
                System.out.println(
                        name + " processing " + job.id +
                                " | instance=" + System.identityHashCode(job)
                );
                Thread.sleep(200); // simulate real work

                boolean success = process(job);
                if(success) {
                    System.out.println(name + " DONE " + job.id);
                    job.completed = true;
                    completedJobs.add(job.id);
                }else {
                    System.out.println(name + " FAILED " + job.id);

                    if(job.canRetry()){
                        job.incrementRetry();

                        long delay = job.nextDelayMillis();

                        System.out.println(name + " RETRYING " + job.id + " in " + delay + "ms" +
                                " (attempt " + job.retryCount + ")");
                        scheduler.schedule(() -> {
                            if(job.completed) {
                                System.out.println("Skipping retry job " + job.id + " already completed");
                                return;
                            }
                            System.out.println("Re-enqueueing " + job.id);
                            queue.offer(job);
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
