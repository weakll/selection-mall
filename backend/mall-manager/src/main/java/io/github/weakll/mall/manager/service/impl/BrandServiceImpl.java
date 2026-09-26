package io.github.weakll.mall.manager.service.impl;

import io.github.weakll.mall.common.cache.CacheConstants;
import io.github.weakll.mall.manager.mapper.BrandMapper;
import io.github.weakll.mall.manager.service.BrandService;
import io.github.weakll.mall.model.entity.product.Brand;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {
    @Autowired
    private BrandMapper brandMapper ;
    @Override
    public PageInfo<Brand> findByPage(Integer page, Integer limit) {
        PageHelper.startPage(page, limit);
        List<Brand> brandList = brandMapper.findByPage() ;
        return new PageInfo(brandList);
    }

    @Override
    @CacheEvict(value = CacheConstants.BRAND_LIST, allEntries = true)
    public void save(Brand brand) {
        brandMapper.save(brand);
    }

    @Override
    @CacheEvict(value = CacheConstants.BRAND_LIST, allEntries = true)
    public void updateById(Brand brand) {
        brandMapper.updateById(brand);
    }

    @Override
    @CacheEvict(value = CacheConstants.BRAND_LIST, allEntries = true)
    public void deleteById(Long id) {
        brandMapper.deleteById(id) ;
    }

    @Override
    public List<Brand> findAll() {
        return brandMapper.findAll() ;
    }
}
