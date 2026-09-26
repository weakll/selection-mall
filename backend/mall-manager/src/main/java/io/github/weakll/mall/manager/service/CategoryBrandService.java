package io.github.weakll.mall.manager.service;

import io.github.weakll.mall.model.dto.product.CategoryBrandDto;
import io.github.weakll.mall.model.entity.product.Brand;
import io.github.weakll.mall.model.entity.product.CategoryBrand;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface CategoryBrandService {
    PageInfo<CategoryBrand> findByPage(Integer page, Integer limit, CategoryBrandDto categoryBrandDto);

    void save(CategoryBrand categoryBrand);

    void updateById(CategoryBrand categoryBrand);

    void deleteById(Long id);

    List<Brand> findBrandByCategoryId(Long categoryId);
}
