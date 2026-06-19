package com.flowforge.engine;

import com.flowforge.core.domain.Job;
import com.flowforge.core.ports.JobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**

 * WorkerPool right now — the full pipeline (submit → rate limit → queue
 * → worker → idempotency → process → events → metrics) needs to be

 */

@Component
public class DefaultJobProcessor implements JobProcessor {
    private static final Logger log = LoggerFactory.getLogger(DefaultJobProcessor.class);
    @Override
    public void process(Job job) throws Exception {

        log.info("[DefaultJobProcessor] Processing type={} payload={}", job.getType(), job.getPayload());
        // N

    }
}
