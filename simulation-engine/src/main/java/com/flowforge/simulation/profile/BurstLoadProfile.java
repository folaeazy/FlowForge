package com.flowforge.simulation.profile;

import java.time.Duration;

/**
 * Low baseline rate, sudden spike, then drop back.
 *
 * Timeline:
 *   0 ──────── baselineEnd: baseline rate (e.g. 10/sec)
 *   baselineEnd ──── burstEnd: peak rate (e.g. 500/sec)
 *   burstEnd ──── totalDuration: back to baseline
 *
 * Example: BurstLoadProfile(10, 500, 30, 10, 120)
 * = baseline 10/sec for 30s, spike to 500/sec for 10s, back to 10/sec until 120s total
 *
 * */
public class BurstLoadProfile implements LoadProfile{

    private final int baselineRate;
    private final int peakRate;
    private final int baselineDurationSeconds;
    private final int burstDurationSeconds;
    private final Duration totalDuration;

    public BurstLoadProfile(int baselineRate, int peakRate, int baselineDurationSeconds, int burstDurationSeconds, int totalDuration) {
        this.baselineRate = baselineRate;
        this.peakRate = peakRate;
        this.baselineDurationSeconds = baselineDurationSeconds;
        this.burstDurationSeconds = burstDurationSeconds;
        this.totalDuration = Duration.ofSeconds(totalDuration);
    }


    @Override
    public int targetRateAt(Duration elapsed) {
        long elapsedSec = elapsed.getSeconds();

        // Phase 1: baseline
        if (elapsedSec < baselineDurationSeconds) {
            return baselineRate;
        }
        // Phase 2: burst
        else if (elapsedSec < baselineDurationSeconds + burstDurationSeconds) {
            return peakRate;
        }
        // Phase 3: back to baseline
        else if (elapsedSec < totalDuration.getSeconds()) {
            return baselineRate;
        }
        // Done
        else {
            return 0;
        }
    }

    @Override
    public Duration totalDuration() {
        return totalDuration;
    }

    @Override
    public String name() {
        return String.format("Burst(baseline=%d/sec for %ds, peak=%d/sec for %ds, total %ds)",
                baselineRate, baselineDurationSeconds, peakRate, burstDurationSeconds, totalDuration.getSeconds());
    }
}
