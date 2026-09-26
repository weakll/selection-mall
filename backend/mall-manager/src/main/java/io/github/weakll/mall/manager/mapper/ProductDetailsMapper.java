package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.entity.product.ProductDetails;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductDetailsMapper {
    void save(ProductDetails productDetails);

    ProductDetails selectByProductId(Long id);

    void updateById(ProductDetails productDetails);

    void deleteByProductId(Long id);
}
