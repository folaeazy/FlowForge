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
    private final IdentityPool identityPool;
    private final UrlPool urlPool;
    private final URLGenerator urlGenerator;

    public ScenarioRunner(FlowForgeApiClient client, LoadProfile profile) {
        this.client = client;
        this.profile = profile;
        this.identityPool = new IdentityPool(5000); // 5000 users
        this.urlPool = new UrlPool(400); //400 urls in pool
        this.urlGenerator = new URLGenerator(urlPool);
    }

    /**
     * Run the scenario from start to finish.
     */
    public SimulationReport run(){
        log.info("=== Starting Simulation: {} ===", profile.name());
        log.info("Total identities: {}", identityPool.totalIdentities());
        log.info("URL pool size: {}", urlPool.size());
        log.info("Total duration: {}", profile.totalDuration());

        Instant start = Instant.now();
        Instant end = start.plus(profile.totalDuration());
        int totalRequests = 0;
        int totalFailures = 0;
        int totalRateLimited = 0;

        while (Instant.now().isBefore(end)) {
            Duration elapsed = Duration.between(start, Instant.now());
            double targetRate = profile.targetRateAt(elapsed);

            int requestsThisSecond = (int) Math.ceil(targetRate);

            log.debug("Elapsed: {}s, target rate: {} req/s, firing {} requests",
                    elapsed.getSeconds(), String.format("%.2f", targetRate), requestsThisSecond);

            // Submit targetRate` jobs in this second
            for(int i = 0; i < requestsThisSecond; i++) {
                try{
                    // pick random identity
                    Identity identity = identityPool.getNextIdentity();
                    if (identity == null) {
                        log.debug("No more identities available");
                        continue;
                    }
                    // Generate Url (distribution 70/20/10)
                    String longUrl = urlGenerator.generateUrl();

                    SimulationRequest request = SimulationRequest.create(identity, longUrl);
                    //Submit to FlowForge API
                    FlowForgeApiClient.SubmitResult result = client.submitJob(request);
                    switch (result) {
                        case ACCEPTED -> totalRequests++;
                        case RATE_LIMITED -> totalRateLimited++;
                        case ERROR -> totalFailures++;
                    }
                } catch (Exception e) {
                    log.error("Error submitting request", e);
                    totalFailures++;
                }
            }
            // Sleep for roughly 1 second to pace the requests
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.warn("Simulation interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        return SimulationReport.print(totalRequests, totalFailures, totalRateLimited);
    }
}
