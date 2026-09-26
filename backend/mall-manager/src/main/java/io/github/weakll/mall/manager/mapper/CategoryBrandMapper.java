package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.dto.product.CategoryBrandDto;
import io.github.weakll.mall.model.entity.product.Brand;
import io.github.weakll.mall.model.entity.product.CategoryBrand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryBrandMapper {
    List<CategoryBrand> findByPage(CategoryBrandDto categoryBrandDto);

    void save(CategoryBrand categoryBrand);

    void updateById(CategoryBrand categoryBrand);

    void deleteById(Long id);

    List<Brand> findBrandByCategoryId(Long categoryId);
}
