package com.flowforge.redis;

import com.flowforge.BaseRedisIntegrationTest;
import com.flowforge.core.domain.RateLimitResult;
import com.flowforge.core.ports.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RedisRateLimiterTest extends BaseRedisIntegrationTest {

    @Autowired
    private RedisRateLimiter redisRateLimiter;
    @Autowired
    private RedisTemplate<String, String > redisTemplate;

    @BeforeEach
    void cleanRedis() { // flush between each test - each test starts with empty redis state
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }

    @Test
    @DisplayName("Allow request within capacity")
    void shouldAllowRequestWithinCapacity() {
        redisRateLimiter.registerTenant("tenant-A", 5, 1);

        for(int i = 0 ;  i < 5; i++) {
            RateLimitResult result = redisRateLimiter.tryAcquire("tenant-A");
            assertThat(result.allowed()).isTrue();
        }
    }


    @Test
    @DisplayName("Rejects when bucket is empty")
    void shouldRejectWhenExhausted() {
        redisRateLimiter.registerTenant("tenant-B", 3, 1);

        for (int i = 0; i < 3; i++) {
            redisRateLimiter.tryAcquire("tenant-B"); // drain
        }

        RateLimitResult rejected = redisRateLimiter.tryAcquire("tenant-B");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.tenantId()).isEqualTo("tenant-B");
    }

    @Test
    @DisplayName("Tenants are isolated — one tenant's limit doesn't affect another")
    void shouldIsolateTenants() {
        redisRateLimiter.registerTenant("tenant-X", 2, 1);
        redisRateLimiter.registerTenant("tenant-Y", 10, 10);

        // Exhaust tenant-X
        redisRateLimiter.tryAcquire("tenant-X");
        redisRateLimiter.tryAcquire("tenant-X");

        // tenant-Y should be unaffected
        assertThat(redisRateLimiter.tryAcquire("tenant-Y").allowed()).isTrue();
        // tenant-X is exhausted
        assertThat(redisRateLimiter.tryAcquire("tenant-X").allowed()).isFalse();
    }

    @Test
    @DisplayName("Refills token over time")
    void shouldRefillTokenAfterDelay() throws InterruptedException{
        redisRateLimiter.registerTenant("tenant-C", 5, 10); // 10/sec = 1 per 100ms

        // Drain entirely
        for (int i = 0; i < 5; i++) redisRateLimiter.tryAcquire("tenant-C");

        assertThat(redisRateLimiter.tryAcquire("tenant-C").allowed()).isFalse();

        Thread.sleep(300);

        assertThat(redisRateLimiter.tryAcquire("tenant-C").allowed()).isTrue();
        assertThat(redisRateLimiter.tryAcquire("tenant-C").allowed()).isTrue();
    }

    @Test
    @DisplayName("Concurrent requests — exactly capacity tokens consumed")
    void shouldHandleConcurrentRequestsSafely() throws InterruptedException{
        redisRateLimiter.registerTenant("tenant-D", 100, 1);
        int threads = 50;
        int requestPerThread = 10;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger(0);

        for(int i = 0; i < threads ; i++) {
            service.submit(() -> {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int j = 0; j < requestPerThread; j++) {
                    if(redisRateLimiter.tryAcquire("tenant-D").allowed()){
                        allowed.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }
        latch.await(10,TimeUnit.SECONDS);
        service.shutdown();

        assertThat(allowed.get()).isEqualTo(100);

    }

    @Test
    @DisplayName("availableTokens reflects current bucket state")
    void shouldReportAvailableTokens() {
        redisRateLimiter.registerTenant("tenant-E", 10, 1);

        assertThat(redisRateLimiter.availableTokens("tenant-E")).isEqualTo(10);

        redisRateLimiter.tryAcquire("tenant-E");
        redisRateLimiter.tryAcquire("tenant-E");

        assertThat(redisRateLimiter.availableTokens("tenant-E")).isEqualTo(8);
    }

}
