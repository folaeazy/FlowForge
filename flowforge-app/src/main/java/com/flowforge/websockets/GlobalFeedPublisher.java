package com.flowforge.websockets;

import com.flowforge.api.dto.ActivityFeedDto;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GlobalFeedPublisher {

    public static final String CHANNEL = "all";
    private static final int ROUTINE_EVENTS_PER_SECOND = 10;

    private final ActivityFeedBroadcaster broadcaster;
    private final AtomicLong windowStartMs = new AtomicLong(0);
    private final AtomicInteger windowCount = new AtomicInteger(0);

    public GlobalFeedPublisher(ActivityFeedBroadcaster broadcaster) { this.broadcaster = broadcaster; }

    public void publish(ActivityFeedDto dto) {
        if (isImportant(dto.type()) || withinBudget()) {
            broadcaster.broadcast(CHANNEL, dto);
        }
    }

    private boolean isImportant(String type) {
        return "ERROR".equals(type) || "RETRY".equals(type) || "DEAD".equals(type);
    }

    private boolean withinBudget() {
        long now = System.currentTimeMillis();
        long start = windowStartMs.get();
        if (now - start >= 1000 && windowStartMs.compareAndSet(start, now)) {
            windowCount.set(0);
        }
        return windowCount.incrementAndGet() <= ROUTINE_EVENTS_PER_SECOND;
    }
}
