package io.github.weakll.mall.product.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.weakll.mall.common.cache.CacheConstants;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CatalogCacheTest {

    @SuppressWarnings("unchecked")
    @Test
    void cachesNullResultWithSentinelToPreventRepeatedMisses() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("catalog::missing")).thenReturn(null);
        CatalogCache cache = new CatalogCache(redis, new ObjectMapper());

        assertNull(cache.getOrLoad("catalog", "missing", Duration.ofMinutes(5), String.class, () -> null));

        verify(values).set(eq("catalog::missing"), eq(CacheConstants.NULL_VALUE), argThat(ttl ->
                ttl.compareTo(Duration.ofMinutes(0)) > 0
                        && ttl.compareTo(Duration.ofMinutes(10)) < 0));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsSentinelAsNullWithoutCallingLoader() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("catalog::missing")).thenReturn(CacheConstants.NULL_VALUE);
        CatalogCache cache = new CatalogCache(redis, new ObjectMapper());

        assertNull(cache.getOrLoad("catalog", "missing", Duration.ofMinutes(5), String.class,
                () -> { throw new AssertionError("loader should not be called"); }));
    }
}
