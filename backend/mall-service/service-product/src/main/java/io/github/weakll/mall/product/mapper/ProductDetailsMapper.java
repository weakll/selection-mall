package io.github.weakll.mall.product.mapper;

import io.github.weakll.mall.model.entity.product.ProductDetails;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductDetailsMapper {
    ProductDetails getByProductId(Long productId);
}
