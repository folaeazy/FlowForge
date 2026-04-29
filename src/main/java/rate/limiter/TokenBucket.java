package rate.limiter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread safe token bucket implementation using CAS Loops
 * Lazy refill - tokens are added on demand not on schedule
 *
 */
public class TokenBucket {
    private static final long PRECISION_FACTOR = 1_000L;

    private final long capacityScaled;
    private final long refillRatePerNano;

    private final AtomicLong tokenScaled;
    private volatile long lastRefillNanos;

    public TokenBucket(long capacityTokens, long tokensPerSecond ) {
        if(capacityTokens <= 0 || tokensPerSecond <= 0) {
            throw new IllegalArgumentException("capacity and rate must be positive");
        }

        this.capacityScaled = capacityTokens * PRECISION_FACTOR;
        this.refillRatePerNano = (tokensPerSecond * PRECISION_FACTOR) / 1_000_000_000L;
        this.tokenScaled = new AtomicLong(capacityScaled); // start full
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * Attempt to consume one token
     *
     * @return true if acquired and false if rate limit exceed
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     *
     * Attempt to consume N tokens atomically
     * using CAS loop - retry without blocking
     */

    public boolean tryAcquire(int tokens) {
        if(tokens <= 0)
            throw new IllegalArgumentException("Token count must be positive");

        refill();

        long required = (long) tokens * PRECISION_FACTOR;
        while (true) {
            long current = tokenScaled.get();

            if(current < required)
                return  false; // not enough token - reject

            if(tokenScaled.compareAndSet(current, current - required))
                return true;

        }


    }

    private  void refill() {
        long now = System.nanoTime();
        long last = lastRefillNanos;
        long elapsedNanos = now - last;

        System.out.println("Elapsed nanos is  " + elapsedNanos);

        if(elapsedNanos <= 0) return;

        long tokensToAdd = elapsedNanos * refillRatePerNano;
        System.out.println("Token to add is  " + tokensToAdd + " " +  refillRatePerNano);
        if(tokensToAdd <= 0) return; // not enough time lapses

        // updated by only one thread
        if(!updateLastRefillTime(last, now)) return;

        tokenScaled.updateAndGet(current ->
                Math.min(capacityScaled, current + tokensToAdd));

    }

    private boolean updateLastRefillTime(long expected, long updated) {

        synchronized (this) {
            if (lastRefillNanos != expected) return false;
            lastRefillNanos = updated;
            return true;
        }
    }

    public long availableTokens() {
        refill();
        return tokenScaled.get() / PRECISION_FACTOR;
    }

    public static void main(String[] args) {

        TokenBucket bucket =  new TokenBucket(10,10);
        bucket.refill();

    }

}
