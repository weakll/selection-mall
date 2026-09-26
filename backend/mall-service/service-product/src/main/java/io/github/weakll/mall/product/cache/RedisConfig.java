package io.github.weakll.mall.product.cache;

import io.github.weakll.mall.common.cache.CacheConstants;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisConfig {
    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringRedisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                CacheConstants.PRODUCT_ITEM, defaultConfig.entryTtl(Duration.ofMinutes(30)),
                CacheConstants.CATEGORY_TREE, defaultConfig.entryTtl(Duration.ofHours(6)),
                CacheConstants.CATEGORY_ONE, defaultConfig.entryTtl(Duration.ofHours(6)),
                CacheConstants.BRAND_LIST, defaultConfig.entryTtl(Duration.ofHours(6))
        );

        RedisCacheWriter cacheWriter = new JitterRedisCacheWriter(
                RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory),
                Duration.ofMinutes(5)
        );

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
