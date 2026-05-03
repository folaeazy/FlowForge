
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rate.limiter.TokenBucket;


import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenBucketTest {

    // ensure bucket starts full

    @Test
    @DisplayName("bucket full")
    void shouldStartWithCapacity() {
        var bucket = new TokenBucket(100, 1000);
        //assertEquals(10, bucket.availableTokens());
        assertThat(100).isEqualTo(bucket.availableTokens());

    }

    // Allow request only when token is available
    @Test
    @DisplayName("successfully acquired token")
    void shouldAllowRequestWhenTokenIsAvailable() {
        var bucket = new TokenBucket(100, 1000);
        for(int i = 0 ; i < 100; i++) {
           assertThat(bucket.tryAcquire()).isTrue();
        }
        assertThat(bucket.tryAcquire()).isFalse();

    }

    // should not exceed cap limit
    @Test
    @DisplayName("capacity limit")
    void shouldNotExceedCapLimit() throws InterruptedException{
        var bucket = new TokenBucket(100, 1000);
        Thread.sleep(2000);
        assertThat(100).isEqualTo(bucket.availableTokens());

    }


    // Reject request when not enough token
    @Test
    @DisplayName("Insufficient token")
    void shouldRejectRequestOnInsufficientToken() {
        var bucket = new TokenBucket(100, 1000);
        assertThat(bucket.tryAcquire(112)).isFalse();
    }

    // Should refill overtime
    @Test
    @DisplayName("refill overtime")
    void shouldRefillOvertime() throws InterruptedException{
        var bucket = new TokenBucket(100, 1000);

        bucket.tryAcquire(100); // empty it
        Thread.sleep(2000); // wait for 2 sec
        assertThat(bucket.availableTokens()).isGreaterThan(0);
    }

    @Test
    @DisplayName("concurrency safety")
    void shouldHandleConcurrentRequestSafely()  throws InterruptedException{
        var bucket = new TokenBucket(100, 1000);
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        int requestPerThread = 10;

        var success = new AtomicInteger(0);
        var latch = new CountDownLatch(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);

        for( int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
                for(int x = 0 ; x < requestPerThread; x++) {
                    if(bucket.tryAcquire())
                        success.incrementAndGet();
                }
                latch.countDown();
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(success.get()).isEqualTo(100);

    }


}
