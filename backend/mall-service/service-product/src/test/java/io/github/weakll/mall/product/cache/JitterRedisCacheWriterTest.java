package io.github.weakll.mall.product.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JitterRedisCacheWriterTest {

    @Test
    void keepsNoExpirationTtl() {
        JitterRedisCacheWriter writer = new JitterRedisCacheWriter(null, Duration.ofMinutes(5));

        assertEquals(Duration.ZERO, writer.jitter(Duration.ZERO));
    }

    @Test
    void appliesJitterWithinConfiguredBounds() {
        Duration ttl = Duration.ofMinutes(30);
        Duration maxJitter = Duration.ofMinutes(5);
        JitterRedisCacheWriter writer = new JitterRedisCacheWriter(null, maxJitter);

        for (int i = 0; i < 200; i++) {
            Duration jittered = writer.jitter(ttl);

            assertTrue(jittered.compareTo(Duration.ofMinutes(25)) >= 0);
            assertTrue(jittered.compareTo(Duration.ofMinutes(35)) <= 0);
        }
    }

    @Test
    void keepsShortTtlPositive() {
        JitterRedisCacheWriter writer = new JitterRedisCacheWriter(null, Duration.ofMinutes(5));

        for (int i = 0; i < 200; i++) {
            Duration jittered = writer.jitter(Duration.ofSeconds(2));

            assertTrue(jittered.compareTo(Duration.ofSeconds(1)) >= 0);
        }
    }
}
