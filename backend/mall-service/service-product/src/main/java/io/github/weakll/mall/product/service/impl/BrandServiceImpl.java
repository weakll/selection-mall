package io.github.weakll.mall.product.service.impl;

import io.github.weakll.mall.common.cache.CacheConstants;
import io.github.weakll.mall.model.entity.product.Brand;
import io.github.weakll.mall.product.mapper.BrandMapper;
import io.github.weakll.mall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Duration;
import io.github.weakll.mall.product.cache.CatalogCache;
@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private CatalogCache catalogCache;

    @Override
    public List<Brand> findAll() {
        return catalogCache.getListOrLoad(
                CacheConstants.BRAND_LIST,
                "all",
                Duration.ofHours(6),
                Brand.class,
                brandMapper::findAll
        );
    }

}
