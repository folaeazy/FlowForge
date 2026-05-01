package rate.limiter;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread safe token bucket implementation with CAS loop
 * Lazy refill - refill on demand not on schedule
 */
public class TokenBucket {

    private final AtomicReference<State> state;
    private final double capacity;
    private final double tokensPerSecond;

    /**
     * Using state for atomic updates
     * Update both token and refill time at the same time
     */
    private static class State {
        final double tokens;
        final long lastRefillTime;

        State(double tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
    }

    public TokenBucket(double capacity, double tokensPerSecond) {
        if(capacity <= 0 || tokensPerSecond <= 0)
            throw new IllegalArgumentException("capacity or tokens must be positive");

        this.capacity = capacity;
        this.tokensPerSecond = tokensPerSecond;
        long now = System.currentTimeMillis();
        this.state = new AtomicReference<>(new State(capacity, now));
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
    private boolean tryAcquire(int requiredTokens) {
        if(requiredTokens <= 0 )
            throw new IllegalArgumentException("tokens must be positive");

        while (true) {
            State current = state.get();
            long now = System.currentTimeMillis();
            long elapsedTime = now - current.lastRefillTime;

            // Tokens to add (refill logic)
            double tokensToAdd = (elapsedTime * tokensPerSecond) / 1000.0; // converted to seconds
            double newToken = Math.min(capacity, current.tokens + tokensToAdd);

            if(newToken < requiredTokens)
                return false;

            // deduct required token
            double remainder = newToken - requiredTokens;

            //updated state
            State updated = new State(remainder, now);

            //CAS attempt
            if(state.compareAndSet(current, updated))
                return true;

        }



    }

}