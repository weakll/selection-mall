package io.github.weakll.mall.manager.service.impl;

import io.github.weakll.mall.manager.mapper.BrandMapper;
import io.github.weakll.mall.manager.cache.CatalogCacheEvictor;
import io.github.weakll.mall.manager.service.BrandService;
import io.github.weakll.mall.model.entity.product.Brand;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {
    @Autowired
    private BrandMapper brandMapper ;
    @Autowired
    private CatalogCacheEvictor catalogCacheEvictor;
    @Override
    public PageInfo<Brand> findByPage(Integer page, Integer limit) {
        PageHelper.startPage(page, limit);
        List<Brand> brandList = brandMapper.findByPage() ;
        return new PageInfo(brandList);
    }

    @Override
    public void save(Brand brand) {
        brandMapper.save(brand);
        catalogCacheEvictor.evictBrands();
    }

    @Override
    public void updateById(Brand brand) {
        brandMapper.updateById(brand);
        catalogCacheEvictor.evictBrands();
    }

    @Override
    public void deleteById(Long id) {
        brandMapper.deleteById(id) ;
        catalogCacheEvictor.evictBrands();
    }

    @Override
    public List<Brand> findAll() {
        return brandMapper.findAll() ;
    }
}
