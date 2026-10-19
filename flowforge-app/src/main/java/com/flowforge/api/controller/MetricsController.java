package com.flowforge.api.controller;

import com.flowforge.api.dto.GatewayResponse;
import com.flowforge.api.dto.JobThroughputResponse;
import com.flowforge.api.dto.MetricsSummaryResponse;
import com.flowforge.core.ports.GlobalStats;
import com.flowforge.core.ports.MetricsStore;
import com.flowforge.engine.JobQueueService;
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
    private final GlobalStats globalStats;
    private final JobQueueService jobQueueService;




    public MetricsController(MetricsStore metricsStore, WorkerPool workerPool, GlobalStats globalStats, JobQueueService jobQueueService) {
        this.metricsStore = metricsStore;
        this.workerPool = workerPool;
        this.globalStats = globalStats;
        this.jobQueueService = jobQueueService;
    }

    @GetMapping
    public MetricsSummaryResponse summary() {
        GlobalStats.Snapshot s = globalStats.snapshot();
        return new MetricsSummaryResponse(
                s.processed(), s.failed(), s.retried(),s.dead(),  jobQueueService.queueSize(), workerPool.activeWorkerCount());
    }

    /** @param minutes how far back the Job Throughput chart looks, default 30 */
    @GetMapping("/throughput")
    public JobThroughputResponse throughput(@RequestParam(defaultValue = "30") int minutes) {
        int clamped = Math.min(Math.max(minutes, 1), 120); // guard against an absurd ?minutes=99999
        return new JobThroughputResponse(metricsStore.getTpsRange(clamped));
    }

    @GetMapping("/gateway")
    public GatewayResponse gateway(@RequestParam(defaultValue = "5") int limit) {
        GlobalStats.Snapshot s = globalStats.snapshot();
        long total = s.admitted() + s.rateLimited();
        double percent = total > 0 ? s.rateLimited() * 100.0 / total : 0.0;
        return new GatewayResponse(s.admitted(), s.rateLimited(), percent,
                globalStats.topThrottled(Math.max(1, Math.min(limit, 20))));
    }
}
