package com.flowforge;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ConsumerRunner {


    private static final Logger log = LoggerFactory.getLogger(ConsumerRunner.class);
    private static final int MAX_URLS = 1000;
    private static final int REDISCOVER_EVERY_SECONDS = 10;

    private final FlowForgeQueryClient client;
    private final Random random = new Random();

    private final List<FlowForgeQueryClient.UrlEntry> known = new ArrayList<>();   // oldest first, stable order
    private final Set<String> knownCodes = new HashSet<>();
    private final Map<FlowForgeQueryClient.Verdict, Long> verdicts = new EnumMap<>(FlowForgeQueryClient.Verdict.class);
    private final LatencyStats hitLatency = new LatencyStats();
    private final LatencyStats missLatency = new LatencyStats();

    public ConsumerRunner(FlowForgeQueryClient client) {
        this.client = client;
    }

    public void run(int durationSeconds, int readsPerSecond) {
        discover();
        if (known.isEmpty()) {
            log.error("No short URLs to consume yet. Run the simulation first.");
            return;
        }
        log.info("Consuming {} reads/s for {}s across {} known URLs", readsPerSecond, durationSeconds, known.size());

        for (int second = 0; second < durationSeconds; second++) {
            long tickStart = System.nanoTime();
            if (second > 0 && second % REDISCOVER_EVERY_SECONDS == 0) discover();

            for (int i = 0; i < readsPerSecond; i++) {
                record(client.probe(pickHot()));
            }

            long spentMs = (System.nanoTime() - tickStart) / 1_000_000;
            try {
                Thread.sleep(Math.max(0, 1000 - spentMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        report();
    }

    /** Adds URLs not seen before, keeping first-discovered ones at the front. */
    private void discover() {
        List<FlowForgeQueryClient.UrlEntry> newestFirst = client.discover(MAX_URLS);
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            FlowForgeQueryClient.UrlEntry e = newestFirst.get(i);
            if (knownCodes.add(e.shortCode())) known.add(e);
        }
    }

    /** Skewed pick: early-discovered URLs are read far more often, like real hot links. */
    private FlowForgeQueryClient.UrlEntry pickHot() {
        int idx = (int) (Math.pow(random.nextDouble(), 3) * known.size());
        return known.get(Math.min(idx, known.size() - 1));
    }

    private void record(FlowForgeQueryClient.ProbeResult r) {
        verdicts.merge(r.verdict(), 1L, Long::sum);
        if (r.verdict() == FlowForgeQueryClient.Verdict.OK) {
            ("HIT".equals(r.cache()) ? hitLatency : missLatency).add(r.latencyMicros());
        } else {
            log.warn("{} status={} code={} location={}", r.verdict(), r.status(), r.entry().shortCode(), r.location());
        }
    }

    private void report() {
        log.info("=== Consumer complete ===");
        verdicts.forEach((v, n) -> log.info("{}: {}", v, n));
        log.info(String.format("%-6s n=%-6d p50=%.1fms p95=%.1fms p99=%.1fms", "HIT",
                hitLatency.count(), hitLatency.percentileMs(50), hitLatency.percentileMs(95), hitLatency.percentileMs(99)));
        log.info(String.format("%-6s n=%-6d p50=%.1fms p95=%.1fms p99=%.1fms", "MISS",
                missLatency.count(), missLatency.percentileMs(50), missLatency.percentileMs(95), missLatency.percentileMs(99)));
        if (hitLatency.count() > 0 && missLatency.count() > 0 && hitLatency.percentileMs(50) > 0) {
            log.info(String.format("Cache speed-up at p50: %.1fx", missLatency.percentileMs(50) / hitLatency.percentileMs(50)));
        }
    }
}
