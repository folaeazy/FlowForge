package job.queue;

import java.util.concurrent.BlockingQueue;

/**
 * Job Consumer or Subscriber
 */
public class Worker implements Runnable{

    private final BlockingQueue<Job> queue;
    private final String name;

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

                        System.out.println(name + " RETRYING " + job.id +
                                " (attempt " + job.retryCount + ")");

                        queue.offer(job);

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
