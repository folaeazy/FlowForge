package job.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Job Consumer or Subscriber
 */
public class Worker implements Runnable{

    private final BlockingQueue<Job> queue;
    private final String name;

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
                System.out.println(name + " processing " + job.id);
                Thread.sleep(200); // simulate real work

                boolean success = process(job);
                if(success) {
                    System.out.println(name + " DONE " + job.id);
                }else {
                    System.out.println(name + " FAILED " + job.id);

                    if(job.canRetry()){
                        job.incrementRetry();

                        long delay = job.nextDelayMillis();

                        System.out.println(name + " RETRYING " + job.id + " in " + delay + "ms" +
                                " (attempt " + job.retryCount + ")");
                        scheduler.schedule(() -> {
                            System.out.println("Re-enqueueing " + job.id);
                            queue.offer(job);
                        }, delay, TimeUnit.MILLISECONDS);
                        if (!queue.offer(job)) {
                            System.out.println("Retry dropped (queue full): " + job.id);
                        }

                    } else {
                    System.out.println(name + " DROPPED " + job.id +
                            " (max retries reached)");
                   }
                }




            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }
}
