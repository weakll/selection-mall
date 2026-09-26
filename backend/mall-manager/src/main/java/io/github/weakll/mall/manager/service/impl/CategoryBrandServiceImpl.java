package io.github.weakll.mall.manager.service.impl;

import io.github.weakll.mall.manager.mapper.CategoryBrandMapper;
import io.github.weakll.mall.manager.service.CategoryBrandService;
import io.github.weakll.mall.model.dto.product.CategoryBrandDto;
import io.github.weakll.mall.model.entity.product.Brand;
import io.github.weakll.mall.model.entity.product.CategoryBrand;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryBrandServiceImpl implements CategoryBrandService {
    @Autowired
    private CategoryBrandMapper categoryBrandMapper ;
    @Override

    public PageInfo<CategoryBrand> findByPage(Integer page, Integer limit, CategoryBrandDto CategoryBrandDto) {
        PageHelper.startPage(page , limit) ;
        List<CategoryBrand> categoryBrandList = categoryBrandMapper.findByPage(CategoryBrandDto) ;
        return new PageInfo<>(categoryBrandList);
    }

    @Override
    public void save(CategoryBrand categoryBrand) {
        categoryBrandMapper.save(categoryBrand) ;
    }

    @Override
    public void updateById(CategoryBrand categoryBrand) {
        categoryBrandMapper.updateById(categoryBrand) ;
    }

    @Override
    public void deleteById(Long id) {
        categoryBrandMapper.deleteById(id) ;
    }

    @Override
    public List<Brand> findBrandByCategoryId(Long categoryId) {
        return categoryBrandMapper.findBrandByCategoryId(categoryId);
    }
}
