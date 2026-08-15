package com.flowforge.simulation.profile;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Randomized rate around a mean, randomized tenant distribution,
 * randomized failure injection — the "realistic, unpredictable" profile.
 *
 * Example: ChaosLoadProfile(50, 20, 120, 0.1)
 * = mean 50 jobs/sec, ±20/sec jitter, run for 120 sec, 10% of jobs fail
 *
 * Every call to targetRateAt() generates a new random rate, so the load
 * actually looks chaotic when plotted over time, not mathematically predictable.
 */
public class ChaosLoadProfile implements LoadProfile{

    private final int meanRate;
    private final int jitterAmount;
    private final Duration duration;
    private final double failureRate;

    public ChaosLoadProfile(int meanRate, int jitterAmount, int duration, double failureRate) {
        this.meanRate = meanRate;
        this.jitterAmount = jitterAmount;
        this.duration = Duration.ofSeconds(duration);
        this.failureRate = failureRate;
    }

    @Override
    public int targetRateAt(Duration elapsed) {
        if (elapsed.compareTo(duration) >= 0) {
            return 0;
        }

        // Random jitter around the mean: meanRate ± jitterAmount
        int jitter = ThreadLocalRandom.current().nextInt(-jitterAmount, jitterAmount + 1);
        return Math.max(1, meanRate + jitter);
    }

    @Override
    public Duration totalDuration() {
        return duration;
    }

    @Override
    public String name() {
        return String.format("Chaos(mean=%d/sec ±%d, %d sec, %.1f%% failure)",
                meanRate, jitterAmount, duration.getSeconds(), failureRate * 100);
    }

    public double getFailureRate() {
        return failureRate;
    }
}
