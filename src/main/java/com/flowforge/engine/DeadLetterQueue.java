package com.flowforge.engine;


import com.flowforge.core.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * In-memory graveyard for jobs that exhausted all retries.
 *
 * KNOWN LIMITATION, STATED EXPLICITLY: this is per-instance, not
 * Redis-backed. In a multi-instance deployment, a job that dies on
 * instance A is invisible to instance B's view. The original spec scoped
 * Redis specifically to idempotency, rate limiting, and metrics — not
 * DLQ — so this is a deliberate, scoped tradeoff. If multi-instance DLQ
 * visibility becomes a real requirement, this is the component to move
 * to a Redis LIST.
 */

@Component
public class DeadLetterQueue {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueue.class);

    private final int maxSize;
    private final ConcurrentLinkedDeque<Job> queue = new ConcurrentLinkedDeque<>();
    private final AtomicInteger size = new AtomicInteger(0);

    public DeadLetterQueue(@Value("${flowforge.dlq.max-size:500}") int maxSize) {
        this.maxSize = maxSize;
    }

    public void park(Job job) {
        if (size.get() >= maxSize) {
            queue.pollFirst();
            size.decrementAndGet();
            log.warn("[DLQ] Full ({}). Evicting oldest entry.", maxSize);
        }
        queue.addLast(job);
        size.incrementAndGet();
        log.warn("[DLQ] Parked: {}", job);
    }

    public List<Job> drainAll() {
        List<Job> drained = queue.stream().collect(Collectors.toList());
        queue.clear();
        size.set(0);
        return drained;
    }

    public List<Job> getAll() {
        return List.copyOf(queue);
    }

    public List<Job> getByTenant(String tenantId) {
        return queue.stream()
                .filter(j -> j.getTenantId().equals(tenantId))
                .collect(Collectors.toList());
    }

    public int size() {
        return size.get();
    }

}
