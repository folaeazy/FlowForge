package com.flowforge.api.dto;

import com.flowforge.core.ports.MetricsStore;

import java.util.List;

public record JobThroughputResponse(
        List<MetricsStore.TpsDataPoint> points
) { }
