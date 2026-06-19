package com.flowforge.engine;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Names threads "job-worker-1", "retry-scheduler-1" instead of the default
 * "pool-1-thread-1" — invaluable in a thread dump when debugging a hang.
 * AtomicInteger (not a plain int, as in the earlier version) since this
 * factory could be shared by more than one pool construction concurrently.
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger count = new AtomicInteger(0);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }


    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, prefix + "-" + count.incrementAndGet());
        t.setDaemon(false);
        return t;
    }
}
