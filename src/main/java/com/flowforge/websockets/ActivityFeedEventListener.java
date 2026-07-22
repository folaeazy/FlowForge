package com.flowforge.websockets;


import com.flowforge.api.dto.ActivityFeedDto;
import com.flowforge.events.JobEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to all JobEvent firings and converts them to ActivityFeedDto.
 *
 * Message format matches the dashboard: [HH:MM:SS] Subject -> Status JobId Details
 * The DTO carries the pieces; the frontend reassembles them for display.
 */
@Component
public class ActivityFeedEventListener {

    private final ActivityFeedBroadcaster broadcaster;

    public ActivityFeedEventListener(ActivityFeedBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @EventListener(JobEvent.class)
    public void onJobEvent(JobEvent event) {
        ActivityFeedDto dto = toDto(event);
        broadcaster.broadcast(event.tenantId(), dto);
    }


    private ActivityFeedDto toDto(JobEvent event) {
        return switch (event) {
            case JobEvent.JobProcessingStarted e -> new ActivityFeedDto(
                    e.timestamp(),
                    "PROCESSING",
                    e.workerId(),
                    e.jobId(),
                    "Attempt " + e.attempt()
            );

            case JobEvent.JobCompleted e -> new ActivityFeedDto(
                    e.timestamp(),
                    "COMPLETED",
                    e.workerId(),
                    e.jobId(),
                    ""
            );

            case JobEvent.JobSkippedDuplicate e -> new ActivityFeedDto(
                    e.timestamp(),
                    "SKIPPED",
                    "System",
                    e.jobId(),
                    "Already processed"
            );

            case JobEvent.JobFailed e -> new ActivityFeedDto(
                    e.timestamp(),
                    "ERROR",
                    e.workerId(),
                    e.jobId(),
                    e.reason()
            );

            case JobEvent.JobRetryScheduled e -> new ActivityFeedDto(
                    e.timestamp(),
                    "RETRY",
                    "System",
                    e.jobId(),
                    "Retrying in " + e.delayMs() + "ms"
            );

            case JobEvent.JobDead e -> new ActivityFeedDto(
                    e.timestamp(),
                    "DEAD",
                    "System",
                    e.jobId(),
                    e.reason()
            );
        };
    }
}
