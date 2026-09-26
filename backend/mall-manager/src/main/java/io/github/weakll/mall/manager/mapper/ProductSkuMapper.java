package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.entity.product.ProductSku;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductSkuMapper {
    void save(ProductSku productSku);

    List<ProductSku> selectByProductId(Long id);

    void updateById(ProductSku productSku);

    void deleteByProductId(Long id);

    List<ProductSku> findProductSkuSale();
}
