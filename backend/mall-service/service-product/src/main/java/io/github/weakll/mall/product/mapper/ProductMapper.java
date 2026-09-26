package io.github.weakll.mall.product.mapper;

import io.github.weakll.mall.model.entity.product.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper {
    Product getById(Long productId);
}
