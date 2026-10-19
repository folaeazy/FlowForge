package com.flowforge.engine;

import com.flowforge.core.ports.GlobalStats;
import com.flowforge.events.JobEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class JobStatsListener {
    private final GlobalStats stats;

    public JobStatsListener(GlobalStats stats) {
        this.stats = stats;
    }

    @EventListener
    public void on(JobEvent event) {
        switch (event) {
            case JobEvent.JobCompleted e      -> stats.incrementProcessed();
            case JobEvent.JobFailed e         -> stats.incrementFailed();       // per failed attempt, like handleFailure
            case JobEvent.JobRetryScheduled e -> stats.incrementRetried();
            case JobEvent.JobDead e           -> stats.incrementDead();
            default -> { }
        }
    }
}
