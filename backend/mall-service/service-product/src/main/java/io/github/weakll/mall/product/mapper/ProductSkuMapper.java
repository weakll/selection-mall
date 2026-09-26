package io.github.weakll.mall.product.mapper;

import io.github.weakll.mall.model.dto.h5.ProductSkuDto;
import io.github.weakll.mall.model.entity.product.ProductSku;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductSkuMapper {
    List<ProductSku> findProductSkuSale();

    ProductSku getById(Long skuId);

    List<ProductSku> findByProductId(Long productId);

    List<ProductSku> findByPage(ProductSkuDto productSkuDto);

    void updateSale(Long skuId, Integer num);
}
