package br.com.fiap.delivery.order.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketTest {

    @Test
    void allowsTheConfiguredBurstAndRejectsTheExcess() {
        TokenBucket bucket = new TokenBucket(20);

        int accepted = 0;
        for (int i = 0; i < 50; i++) {
            if (bucket.tryAcquire()) {
                accepted++;
            }
        }

        // 20 tokens available at once; at most one more can be refilled while the loop runs
        assertThat(accepted).isBetween(20, 21);
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(20);
        for (int i = 0; i < 20; i++) {
            bucket.tryAcquire();
        }
        assertThat(bucket.tryAcquire()).isFalse();

        Thread.sleep(150); // 20 tokens per second -> about 3 tokens in 150 ms

        assertThat(bucket.tryAcquire()).isTrue();
    }
}
