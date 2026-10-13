package br.com.fiap.delivery.order.config;

final class TokenBucket {

    private final double capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastRefill;

    TokenBucket(int permitsPerSecond) {
        if (permitsPerSecond < 1) {
            throw new IllegalArgumentException("permitsPerSecond must be at least 1");
        }
        this.capacity = permitsPerSecond;
        this.refillPerNano = permitsPerSecond / 1_000_000_000.0;
        this.tokens = permitsPerSecond;
        this.lastRefill = System.nanoTime();
    }

    synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
        lastRefill = now;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }
}
