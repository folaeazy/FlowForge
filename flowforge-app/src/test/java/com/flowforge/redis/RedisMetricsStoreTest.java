package com.flowforge.redis;

import com.flowforge.BaseRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RedisMetricsStoreTest extends BaseRedisIntegrationTest {

    @Autowired private RedisMetricsStore metricsStore;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }

    @Test
    @DisplayName("Processed counter increments per tenant")
    void shouldIncrementProcessedPerTenant() {
        metricsStore.incrementProcessed("tenant-A");
        metricsStore.incrementProcessed("tenant-A");
        metricsStore.incrementProcessed("tenant-B");

        assertThat(metricsStore.getProcessed("tenant-A")).isEqualTo(2);
        assertThat(metricsStore.getProcessed("tenant-B")).isEqualTo(1);
    }

    @Test
    @DisplayName("Failed and retry counters are independent")
    void shouldTrackFailedAndRetriesSeparately() {
        metricsStore.incrementProcessed("tenant-A");
        metricsStore.incrementFailed("tenant-A");
        metricsStore.incrementRetried("tenant-A");

        assertThat(metricsStore.getProcessed("tenant-A")).isEqualTo(1);
        assertThat(metricsStore.getFailed("tenant-A")).isEqualTo(1);
        assertThat(metricsStore.getRetried("tenant-A")).isEqualTo(1);
    }

    @Test
    @DisplayName("Unknown tenant returns zero — not an error")
    void shouldReturnZeroForUnknownTenant() {
        assertThat(metricsStore.getProcessed("nobody")).isEqualTo(0);
        assertThat(metricsStore.getFailed("nobody")).isEqualTo(0);
        assertThat(metricsStore.getRetried("nobody")).isEqualTo(0);
    }

    @Test
    @DisplayName("Queue size is recorded and readable")
    void shouldRecordQueueSize() {
        metricsStore.recordQueueSize(256);
        String raw = redisTemplate.opsForValue().get(RedisKeys.QUEUE_SIZE);
        assertThat(raw).isEqualTo("256");
    }
}
