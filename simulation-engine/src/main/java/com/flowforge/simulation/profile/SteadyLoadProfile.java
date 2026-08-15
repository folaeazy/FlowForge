package com.flowforge.simulation.profile;

import java.time.Duration;

/**
 * Constant rate for a fixed duration.
 *
 * Example: SteadyLoadProfile(50, 300 seconds)
 * = hold exactly 50 jobs/sec for 5 minutes
 */
public class SteadyLoadProfile implements LoadProfile{

    private final int jobsPerSecond;
    private final Duration duration;

    public SteadyLoadProfile(int jobsPerSecond, int duration) {
        this.jobsPerSecond = jobsPerSecond;
        this.duration = Duration.ofSeconds(duration);
    }

    @Override
    public int targetRateAt(Duration elapsed) {
        // If we've gone past the end, return 0 (stop submitting)
        return elapsed.compareTo(duration) >= 0 ? 0 : jobsPerSecond;
    }


    @Override
    public Duration totalDuration() {
        return duration;
    }

    @Override
    public String name() {
        return String.format("Steady(%d/sec, %d sec)", jobsPerSecond, duration.getSeconds());
    }
}
