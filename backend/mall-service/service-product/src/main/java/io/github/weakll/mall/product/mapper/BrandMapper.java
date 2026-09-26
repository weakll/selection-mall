package io.github.weakll.mall.product.mapper;

import io.github.weakll.mall.model.entity.product.Brand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface BrandMapper {
    List<Brand> findAll();
}
