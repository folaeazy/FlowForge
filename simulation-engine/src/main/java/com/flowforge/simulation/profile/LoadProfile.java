package com.flowforge.simulation.profile;

import java.time.Duration;

/**
 * Defines a traffic shape over time.
 *
 * The contract: at any given elapsed time within totalDuration(),
 * targetRateAt(elapsed) returns how many jobs/second the scenario should
 * be submitting in that window.
 *
 * Example: RampingLoadProfile(10, 200, 60 seconds) returns:
 *   - elapsed=0s → 10 jobs/sec
 *   - elapsed=30s → 105 jobs/sec (halfway through the ramp)
 *   - elapsed=60s → 200 jobs/sec (ramp complete)
 */
public interface LoadProfile {

    /**
     * Target submission rate (jobs/second) at a given elapsed time.
     * The ScenarioRunner uses this every second to decide how many
     * jobs to submit in that window.
     */
    int targetRateAt(Duration elapsed);

    /**
     * How long this profile should run.
     */
    Duration totalDuration();

    /**
     * Human-readable name for logging/reports.
     */
    String name();

}
