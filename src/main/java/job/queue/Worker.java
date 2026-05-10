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

    @Override
    public void run() {
        while (true) {
            try {

                Job job = queue.take();
                System.out.println(name + " processing " + job.id);

                Thread.sleep(200); // simulate real work

                System.out.println(name + " DONE " + job.id);
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }
}
