package io.github.weakll.mall.product.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.weakll.mall.common.cache.CacheConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CatalogCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CatalogCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T getOrLoad(String cacheName, String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        String redisKey = cacheName + "::" + key;
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            if (CacheConstants.NULL_VALUE.equals(cached)) {
                return null;
            }
            try {
                return objectMapper.readValue(cached, type);
            } catch (JsonProcessingException exception) {
                redisTemplate.delete(redisKey);
            }
        }
        T loaded = loader.get();
        put(redisKey, loaded, ttl);
        return loaded;
    }

    public <T> List<T> getListOrLoad(String cacheName, String key, Duration ttl, Class<T> elementType,
                                     Supplier<List<T>> loader) {
        String redisKey = cacheName + "::" + key;
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            if (CacheConstants.NULL_VALUE.equals(cached)) {
                return null;
            }
            try {
                JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
                return objectMapper.readValue(cached, listType);
            } catch (JsonProcessingException exception) {
                redisTemplate.delete(redisKey);
            }
        }
        List<T> loaded = loader.get();
        put(redisKey, loaded, ttl);
        return loaded;
    }

    public void evict(String cacheName, String key) {
        redisTemplate.delete(cacheName + "::" + key);
    }

    public void evictAll(String cacheName) {
        List<String> keys = new ArrayList<>();
        redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(cacheName + "::*")
                .count(200)
                .build()).forEachRemaining(keys::add);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void put(String key, Object value, Duration ttl) {
        String payload;
        try {
            payload = value == null ? CacheConstants.NULL_VALUE : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return;
        }
        redisTemplate.opsForValue().set(key, payload, jitter(ttl));
    }

    private Duration jitter(Duration ttl) {
        long offset = ThreadLocalRandom.current().nextLong(-300, 301);
        Duration result = ttl.plusSeconds(offset);
        return result.isZero() || result.isNegative() ? Duration.ofSeconds(1) : result;
    }
}
