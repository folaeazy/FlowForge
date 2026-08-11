package com.flowforge.simulation.profile;

import java.time.Duration;

/**
 * Linearly increase rate from min to max over the duration.
 *
 * Example: RampingLoadProfile(10, 300, 120)
 * = start at 10/sec, linearly ramp to 300/sec over 120 seconds
 *   at 60s (halfway): ~155 jobs/sec
 *   at 120s: 300/sec
 */
public class RampingLoadProfile implements LoadProfile{

    private final int minRate;
    private final int maxRate;
    private final Duration duration;

    public RampingLoadProfile(int minRate, int maxRate, Duration duration) {
        this.minRate = minRate;
        this.maxRate = maxRate;
        this.duration = duration;
    }

    @Override
    public int targetRateAt(Duration elapsed) {
        long elapsedSec = elapsed.getSeconds();
        long totalSec = duration.getSeconds();

        if (elapsedSec >= totalSec) {
            return 0;
        }

        // Linear interpolation: min + (max - min) * (elapsed / total)
        double progress = (double) elapsedSec / totalSec;
        return (int) (minRate + (maxRate - minRate) * progress);
    }

    @Override
    public Duration totalDuration() {
        return duration;
    }

    @Override
    public String name() {
        return String.format("Ramping(%d → %d/sec over %d sec)", minRate, maxRate, duration.getSeconds());
    }
}
