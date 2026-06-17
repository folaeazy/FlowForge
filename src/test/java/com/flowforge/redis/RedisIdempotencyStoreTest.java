package com.flowforge.redis;

import com.flowforge.BaseRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RedisIdempotencyStoreTest extends BaseRedisIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisIdempotencyStore idempotencyStore;

    @BeforeEach
    void cleanRedis() { // flush between each test - each test starts with empty redis state
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }

    @Test
    @DisplayName("Unprocessed job should return false")
    void shouldReturnFalseForUnseenJob(){
        assertThat(idempotencyStore.isProcessed("Job-Sample-None")).isFalse();
    }

    @Test
    @DisplayName("Marked job is detected as processed")
    void shouldDetectProcessedJob() {
        String jobId = "Job-abc-123";
        assertThat(idempotencyStore.isProcessed(jobId)).isFalse();
        idempotencyStore.markProcessed(jobId, 3600);
        assertThat(idempotencyStore.isProcessed(jobId)).isTrue();
    }


    @Test
    @DisplayName("Calling markProcessed twice is safe — idempotent")
    void markProcessedIsIdempotent() {
        String jobId = "job-duplicate-789";

        idempotencyStore.markProcessed(jobId, 3600);
        idempotencyStore.markProcessed(jobId, 3600); // second call — must not throw

        assertThat(idempotencyStore.isProcessed(jobId)).isTrue();
    }

    @Test
    @DisplayName("Different job IDs are independent")
    void shouldIsolateJobIds() {
        idempotencyStore.markProcessed("job-1", 3600);

        assertThat(idempotencyStore.isProcessed("job-1")).isTrue();
        assertThat(idempotencyStore.isProcessed("job-2")).isFalse();
    }

    @Test
    @DisplayName("Should expire at ttl")
    void shouldExpireAfterTll() throws InterruptedException {
        idempotencyStore.markProcessed("job-123", 1);
        assertThat(idempotencyStore.isProcessed("job-123")).isTrue();

        Thread.sleep(1500);
        assertThat(idempotencyStore.isProcessed("job-123")).isFalse();

    }
}
