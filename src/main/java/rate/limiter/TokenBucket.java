package rate.limiter;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread safe token bucket implementation with CAS loop
 * Lazy refill - refill on demand not on schedule
 */
public class TokenBucket {

    private final AtomicReference<State> state;
    private final long capacityScaled;
    private final long ratePerNano;
    private static final long  SCALE = 1_000_000L;

    /**
     * Using state for atomic updates
     * Update both token and refill time at the same time
     */
    private static class State {
        final long tokens;
        final long lastRefillTime;

        State(long tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
    }

    public  TokenBucket(long capacity, long tokensPerSecond) {
        if(capacity <= 0 || tokensPerSecond <= 0)
            throw new IllegalArgumentException("capacity or tokens must be positive");

        this.capacityScaled = capacity * SCALE;
        this.ratePerNano = (tokensPerSecond * SCALE) / 1_000_000_000L; // converted to nanoseconds;

        if (this.ratePerNano <= 0)
            throw new IllegalArgumentException(
                    "Rate too low for nanosecond precision. Minimum: 1 token/sec with SCALE=" + SCALE
            );
        long now = System.nanoTime();
        this.state = new AtomicReference<>(new State(capacityScaled, now));
    }

    /**
     * Attempt to consume 1 token
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * Attempt to consume N number of tokens
     *
     */
    public boolean tryAcquire(int tokens) {
        if(tokens <= 0 )
            throw new IllegalArgumentException("tokens must be positive");

        long required = tokens * SCALE;
        while (true) {
            State current = state.get();


            // Tokens to add (refill logic)
            State refreshed = refill(current);

            if(refreshed.tokens < required)
                return false;

            // deduct required token
            long remainder = refreshed.tokens - required;

            //updated state
            State updated = new State(remainder, refreshed.lastRefillTime);

            //CAS attempt
            if(state.compareAndSet(current, updated))
                return true;

        }

    }

    /**
     * Get available token updated with time
     * Using CAS Loop for state consistency
     */
    public long availableTokens() {
        while (true) {
            State current = state.get();
            State refreshed = refill(current);

            if(state.compareAndSet(current, refreshed))
                return refreshed.tokens / SCALE;
        }

    }

    private State refill(State current) {
        long now = System.nanoTime();
        long elapsedTime = now - current.lastRefillTime;

        if(elapsedTime <= 0)
            return current;

        long tokensToAdd = elapsedTime * ratePerNano; // converted to seconds

        if(tokensToAdd <= 0)
            return current;

        long newToken = Math.min(capacityScaled, current.tokens + tokensToAdd); // cap at maximum

        return new State(newToken, now);



    }

    public static void main(String[] a)  {
        var bucket = new TokenBucket(100, 1000);
        System.out.println(bucket.availableTokens());


    }

}