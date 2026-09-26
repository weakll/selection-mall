package io.github.weakll.mall.product.mapper;

import io.github.weakll.mall.model.entity.product.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<Category> findOneCategory();

    List<Category> findAll();
}
