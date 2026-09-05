package com.flowforge.api;

import com.flowforge.BaseRedisIntegrationTest;
import com.flowforge.api.controller.MetricsController;
import com.flowforge.core.ports.MetricsStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MetricsControllerTest extends BaseRedisIntegrationTest {

    @Autowired private MetricsController controller;
    @Autowired
    private MetricsStore metricsStore;

    //@Test
    void summaryReflectsRecordedMetrics() {
        metricsStore.incrementProcessed("tenant-A");
        metricsStore.incrementFailed("tenant-A");

        var summary = controller.summary();

        assertThat(summary.processed()).isGreaterThanOrEqualTo(1);
        assertThat(summary.failed()).isGreaterThanOrEqualTo(1);
    }

    //@Test
    void throughputReturnsRequestedNumberOfMinuteBuckets() {
        var response = controller.throughput(10);
        assertThat(response.points()).isEqualTo(10);
    }
}
