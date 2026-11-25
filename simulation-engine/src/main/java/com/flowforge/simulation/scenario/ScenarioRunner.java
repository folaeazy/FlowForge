package com.flowforge.simulation.scenario;

import com.flowforge.simulation.client.FlowForgeApiClient;
import com.flowforge.simulation.identity.Identity;
import com.flowforge.simulation.identity.IdentityPool;
import com.flowforge.simulation.profile.LoadProfile;
import com.flowforge.simulation.report.SimulationReport;
import com.flowforge.simulation.request.SimulationRequest;
import com.flowforge.simulation.url.URLGenerator;
import com.flowforge.simulation.url.UrlPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com. flowforge. simulation. client. FlowForgeApiClient. SubmitResult;
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

    private final LoadProfile profile;
    private final FlowForgeApiClient apiClient;
    private final IdentityPool identityPool = new IdentityPool();
    private final URLGenerator urlGenerator = new URLGenerator(new UrlPool(300));

    private final long[][] tally =
            new long[IdentityPool.Category.values().length][SubmitResult.values().length];

    public ScenarioRunner(LoadProfile profile, FlowForgeApiClient apiClient) {
        this.profile = profile;
        this.apiClient = apiClient;
    }

    public void run() {
        log.info("=== Starting simulation: {} for {} ===", profile.name(), profile.totalDuration());

        Instant start = Instant.now();
        Instant end = start.plus(profile.totalDuration());

        while (Instant.now().isBefore(end)) {
            long tickStart = System.nanoTime();
            Duration elapsed = Duration.between(start, Instant.now());
            int toFire = (int) Math.ceil(profile.targetRateAt(elapsed));

            for (Identity identity : identityPool.nextBatch(toFire)) {
                SubmitResult result;
                try {
                    result = apiClient.submitJob(SimulationRequest.create(identity, urlGenerator.generateUrl()));
                } catch (Exception e) {
                    log.error("Unexpected failure submitting request", e);
                    result = SubmitResult.ERROR;
                }
                tally[identity.category().ordinal()][result.ordinal()]++;
            }

            long spentMs = (System.nanoTime() - tickStart) / 1_000_000;
            if (spentMs > 1000) {
                log.warn("Tick took {} ms for {} requests: the single-threaded client can't sustain this rate", spentMs, toFire);
            }
            try {
                Thread.sleep(Math.max(0, 1000 - spentMs));   // sleep only the rest of the second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        printReport();
    }

    private void printReport() {
        log.info("=== Simulation complete ===");
        log.info(String.format("%-10s %10s %12s %10s %8s", "category", "accepted", "rateLimited", "queueFull", "errors"));
        long[] totals = new long[SubmitResult.values().length];
        for (IdentityPool.Category c : IdentityPool.Category.values()) {
            long[] row = tally[c.ordinal()];
            log.info(String.format("%-10s %10d %12d %10d %8d", c,
                    row[SubmitResult.ACCEPTED.ordinal()], row[SubmitResult.RATE_LIMITED.ordinal()],
                    row[FlowForgeApiClient.SubmitResult.QUEUE_FULL.ordinal()], row[FlowForgeApiClient.SubmitResult.ERROR.ordinal()]));
            for (int i = 0; i < row.length; i++) totals[i] += row[i];
        }
        log.info(String.format("%-10s %10d %12d %10d %8d", "TOTAL",
                totals[SubmitResult.ACCEPTED.ordinal()], totals[FlowForgeApiClient.SubmitResult.RATE_LIMITED.ordinal()],
                totals[FlowForgeApiClient.SubmitResult.QUEUE_FULL.ordinal()], totals[FlowForgeApiClient.SubmitResult.ERROR.ordinal()]));
    }
}
