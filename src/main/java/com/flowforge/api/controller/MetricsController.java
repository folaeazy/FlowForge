package com.flowforge.api.controller;

import com.flowforge.api.dto.JobThroughputResponse;
import com.flowforge.api.dto.MetricsSummaryResponse;
import com.flowforge.core.ports.MetricsStore;
import com.flowforge.engine.WorkerPool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Backs the 5 top metric cards and the Job Throughput chart.
 *
 * Note: "processed/failed/retried" here are GLOBAL totals (summed across
 * all tenants), not per-tenant — the dashboard's top cards show system-wide
 * numbers. Per-tenant breakdowns live in TenantController instead.
 */

@RestController
@RequestMapping("api/metrics")
public class MetricsController {

    private final MetricsStore metricsStore;
    private final WorkerPool workerPool;

    // Tenants currently known to the system. In a real system this would
    // come from a TenantRegistry; for now it's the same demo set used
    // elsewhere until a proper tenant store exists.
    private static final List<String> KNOWN_TENANTS =
            List.of("tenant-A", "tenant-B", "tenant-C", "tenant-D", "tenant-E");


    public MetricsController(MetricsStore metricsStore, WorkerPool workerPool) {
        this.metricsStore = metricsStore;
        this.workerPool = workerPool;
    }

    @GetMapping
    public MetricsSummaryResponse summary() {
        long processed = KNOWN_TENANTS.stream().mapToLong(metricsStore::getProcessed).sum();
        long failed = KNOWN_TENANTS.stream().mapToLong(metricsStore::getFailed).sum();
        long retried = KNOWN_TENANTS.stream().mapToLong(metricsStore::getRetried).sum();

        return new MetricsSummaryResponse(processed, failed, retried, metricsStore.getQueueSize(), workerPool.activeWorkerCount());
    }

    /** @param minutes how far back the Job Throughput chart looks, default 30 */
    @GetMapping("/throughput")
    public JobThroughputResponse throughput(@RequestParam(defaultValue = "30") int minutes) {
        int clamped = Math.min(Math.max(minutes, 1), 120); // guard against an absurd ?minutes=99999
        return new JobThroughputResponse(metricsStore.getTpsRange(clamped));
    }
}
