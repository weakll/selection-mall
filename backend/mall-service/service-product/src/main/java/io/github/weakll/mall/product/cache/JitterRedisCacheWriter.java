package io.github.weakll.mall.product.cache;

import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class JitterRedisCacheWriter implements RedisCacheWriter {

    private final RedisCacheWriter delegate;
    private final Duration maxJitter;

    public JitterRedisCacheWriter(RedisCacheWriter delegate, Duration maxJitter) {
        this.delegate = delegate;
        this.maxJitter = maxJitter;
    }

    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        delegate.put(name, key, value, jitter(ttl));
    }

    @Override
    public byte[] get(String name, byte[] key) {
        return delegate.get(name, key);
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.putIfAbsent(name, key, value, jitter(ttl));
    }

    @Override
    public void remove(String name, byte[] key) {
        delegate.remove(name, key);
    }

    @Override
    public void clean(String name, byte[] pattern) {
        delegate.clean(name, pattern);
    }

    @Override
    public void clearStatistics(String name) {
        delegate.clearStatistics(name);
    }

    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector cacheStatisticsCollector) {
        return new JitterRedisCacheWriter(
                delegate.withStatisticsCollector(cacheStatisticsCollector),
                maxJitter
        );
    }

    @Override
    public CacheStatistics getCacheStatistics(String cacheName) {
        return delegate.getCacheStatistics(cacheName);
    }

    Duration jitter(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return ttl;
        }

        long maxJitterSeconds = maxJitter.toSeconds();
        if (maxJitterSeconds <= 0) {
            return ttl;
        }

        long offset = ThreadLocalRandom.current().nextLong(
                -maxJitterSeconds,
                maxJitterSeconds + 1
        );
        Duration jittered = ttl.plusSeconds(offset);
        return jittered.isNegative() || jittered.isZero() ? Duration.ofSeconds(1) : jittered;
    }
}
