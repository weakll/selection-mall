package io.github.weakll.mall.product.service.impl;

import io.github.weakll.mall.common.cache.CacheConstants;
import io.github.weakll.mall.model.entity.product.Brand;
import io.github.weakll.mall.product.mapper.BrandMapper;
import io.github.weakll.mall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandMapper brandMapper;

    @Cacheable(value = CacheConstants.BRAND_LIST, key = "'all'")
    @Override
    public List<Brand> findAll() {
        return brandMapper.findAll();
    }

}
