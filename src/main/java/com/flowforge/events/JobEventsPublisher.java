package com.flowforge.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring's ApplicationEventPublisher.
 *
 * Two reasons this exists instead of injecting ApplicationEventPublisher
 * directly into JobWorker:
 *
 * 1. One chokepoint to log every event uniformly — call sites don't each
 *    need to remember to log.
 * 2. If the events/ layer ever moves to a real broker (Kafka, RabbitMQ),
 *    only this class changes. Engine code keeps calling publish(event)
 *    without knowing or caring what's underneath.
 */
@Component
public class JobEventsPublisher {
    private static final Logger log = LoggerFactory.getLogger(JobEventsPublisher.class);
    private final ApplicationEventPublisher delegate;

    public JobEventsPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }
    public void publish(JobEvent event) {
        log.debug("[Event] {} jobId={} tenant={}",
                event.getClass().getSimpleName(), event.jobId(), event.tenantId());

        delegate.publishEvent(event);
    }
}
