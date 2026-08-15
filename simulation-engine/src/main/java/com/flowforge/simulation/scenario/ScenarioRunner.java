package com.flowforge.simulation.scenario;

import com.flowforge.simulation.client.FlowForgeApiClient;
import com.flowforge.simulation.profile.LoadProfile;
import com.flowforge.simulation.report.SimulationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes a LoadProfile scenario against FlowForge.
 *
 * Algorithm:
 *   1. Determine totalDuration from profile
 *   2. Loop every second:
 *      a. Ask profile: how many jobs should we submit right now?
 *      b. Submit that many jobs (sequentially)
 *      c. Collect results (accepted/rate-limited/queue-full)
 *      d. Sleep until the next second
 *   3. After totalDuration elapses, stop
 *   4. Generate a report
 *
 * This is deliberately single-threaded — we want to control the exact rate,
 * and threading adds complexity. For 300 jobs/sec we'd need more threads,
 * but that's a future optimization.
 */
public class ScenarioRunner {

    private static final Logger log = LoggerFactory.getLogger(ScenarioRunner.class);

    private final FlowForgeApiClient client;
    private final LoadProfile profile;
    private final List<String> tenants;

    public ScenarioRunner(FlowForgeApiClient client, LoadProfile profile) {
        this.client = client;
        this.profile = profile;
        this.tenants = List.of("tenant-A", "tenant-B", "tenant-C", "tenant-D", "tenant-E");
    }

    /**
     * Run the scenario from start to finish.
     *
     * @param failureRate 0.0–1.0, fraction of jobs to flag as simulateFailure=true
     * @return a SimulationReport with summary statistics
     */
    public SimulationReport run(double failureRate){
        log.info("[Scenario] Starting: {}", profile.name());

        Instant startTime = Instant.now();
        Duration totalDuration = profile.totalDuration();

        AtomicInteger accepted = new AtomicInteger(0);
        AtomicInteger rateLimited = new AtomicInteger(0);
        AtomicInteger queueFull = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        int totalSubmitted = 0;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int maxRateObserved = 0;

        while (true) {
            Duration elapsed = Duration.between(startTime, Instant.now());

            if (elapsed.compareTo(totalDuration) >= 0) {
                log.info("[Scenario] Completed in {}", elapsed);
                break;
            }

            int targetRate = profile.targetRateAt(elapsed);
            maxRateObserved = Math.max(maxRateObserved, targetRate);

            // Submit `targetRate` jobs in this second
            for (int i = 0; i < targetRate; i++) {
                String tenant = tenants.get(random.nextInt(tenants.size()));
                boolean shouldFail = random.nextDouble() < failureRate;

                FlowForgeApiClient.SubmitResult result = client.submitJob(
                        tenant,
                        "TEST_JOB",
                        null,
                        shouldFail
                );

                switch (result) {
                    case ACCEPTED -> accepted.incrementAndGet();
                    case RATE_LIMITED -> rateLimited.incrementAndGet();
                    case QUEUE_FULL -> queueFull.incrementAndGet();
                    case ERROR -> errors.incrementAndGet();
                }

                totalSubmitted++;
            }

            // Sleep until the next second
            long elapsedMs = elapsed.toMillis();
            long nextSecondMs = ((elapsedMs / 1000) + 1) * 1000;
            long sleepMs = nextSecondMs - elapsedMs;

            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[Scenario] Interrupted");
                    break;
                }
            }
        }

        Instant endTime = Instant.now();
        Duration actualDuration = Duration.between(startTime, endTime);

        return new SimulationReport(
                profile.name(),
                actualDuration,
                totalSubmitted,
                accepted.get(),
                rateLimited.get(),
                queueFull.get(),
                errors.get(),
                maxRateObserved
        );
    }
}
