
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rate.limiter.TokenBucket;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenBucketTest {

    // ensure bucket starts full
//
//    @Test
//    @DisplayName("bucket full")
//    void shouldStartWithCapacity() {
//        var bucket = new TokenBucket(10.0, 5.0);
//        //assertEquals(10, bucket.availableTokens());
//        assertThat(10.0).isEqualTo(bucket.availableTokens());
//
//    }
//
//    // Allow request only when token is available
//    @Test
//    @DisplayName("successfully acquired token")
//    void shouldAllowRequestWhenTokenIsAvailable() {
//        var bucket = new TokenBucket(10.0, 5.0);
//        for(int i = 0 ; i < 10; i++) {
//           assertThat(bucket.tryAcquire()).isTrue();
//        }
//        assertThat(bucket.tryAcquire()).isFalse();
//
//    }
//
//    // should not exceed cap limit
//    @Test
//    @DisplayName("capacity limit")
//    void shouldNotExceedCapLimit() throws InterruptedException{
//        var bucket = new TokenBucket(10.0, 5.0);
//        Thread.sleep(2000);
//        assertThat(10.0).isEqualTo(bucket.availableTokens());
//
//    }
//
//
//    // Reject request when not enough token
//    @Test
//    @DisplayName("Insufficient token")
//    void shouldRejectRequestOnInsufficientToken() {
//        var bucket = new TokenBucket(10.0, 5.0);
//        assertThat(bucket.tryAcquire(12)).isFalse();
//    }
//
//    // Should refill overtime
//    @Test
//    @DisplayName("refill overtime")
//    void shouldRefillOvertime() throws InterruptedException{
//        var bucket = new TokenBucket(10.0, 5.0);
//
//        bucket.tryAcquire(10); // empty it
//        Thread.sleep(2000); // wait for 2 sec
//        assertThat(bucket.availableTokens()).isGreaterThan(0);
//    }

    @Test
    @DisplayName("concurrency safety")
    void shouldHandleConcurrentRequestSafely()  throws InterruptedException{
        var bucket = new TokenBucket(100, 10);
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        int requestPerThread = 10;

        var success = new AtomicInteger(0);
        var latch = new CountDownLatch(threads);

        for( int i = 0; i < threads; i++) {
            executor.submit(() -> {
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
