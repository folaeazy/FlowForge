package com.flowforge.config;

import com.flowforge.core.domain.Job;
import com.flowforge.engine.NamedThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Wires the infrastructure that JobQueueService (producer) and WorkerPool
 * (consumer) both need a reference to the SAME instance of.
 *
 * If JobQueueService constructed the queue itself, WorkerPool would need a
 * setter or some other indirection to get the identical instance. Declaring
 * it as a @Bean means both components just ask for a BlockingQueue<Job> in
 * their constructor and Spring guarantees the same singleton — no plumbing.
 */
@Configuration
public class EngineConfig {

    @Bean
    public BlockingQueue<Job> blockingQueue(@Value("${flowforge.queue.capacity:1000}") int capacity) {
        return new LinkedBlockingDeque<>(capacity);
    }

    @Bean
    public ScheduledExecutorService retryScheduler() {
        // corePoolSize=2: retries are infrequent relative to normal
        // processing — generous headroom without spawning unbounded threads.
        return Executors.newScheduledThreadPool(2, new NamedThreadFactory("retry-scheduler"));
    }


}
