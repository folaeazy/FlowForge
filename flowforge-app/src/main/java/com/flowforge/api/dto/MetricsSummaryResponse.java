package com.flowforge.api.dto;

public record MetricsSummaryResponse(
        long processed,
        long failed,
        long retried,
        long dead,
        long queueSize,
        int activeWorkers
) { }
