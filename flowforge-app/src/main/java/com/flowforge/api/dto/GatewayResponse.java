package com.flowforge.api.dto;

import com.flowforge.core.ports.GlobalStats;
import java.util.List;

public record GatewayResponse(
        long admitted, long rateLimited, double rateLimitedPercent,
        List<GlobalStats.ThrottledIdentity> topThrottled) { }