package io.github.weakll.mall.manager.cache;

import io.github.weakll.mall.common.cache.CacheConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CatalogCacheEvictor {

    private final StringRedisTemplate redisTemplate;

    public CatalogCacheEvictor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void evictProductItems() {
        evict(CacheConstants.PRODUCT_ITEM);
    }

    public void evictCategories() {
        evict(CacheConstants.CATEGORY_TREE);
        evict(CacheConstants.CATEGORY_ONE);
    }

    public void evictBrands() {
        evict(CacheConstants.BRAND_LIST);
    }

    private void evict(String cacheName) {
        Set<String> keys = redisTemplate.keys(cacheName + "::*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
