package io.github.weakll.mall.product.service;

import io.github.weakll.mall.model.dto.h5.ProductSkuDto;
import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.vo.h5.ProductItemVo;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface ProductService {
    List<ProductSku> findProductSkuSale();

    ProductItemVo item(Long skuId);

    PageInfo<ProductSku> findByPage(Integer page, Integer limit, ProductSkuDto productSkuDto);

    ProductSku getBySkuId(Long skuId);

    Boolean updateSkuSaleNum(List<SkuSaleDto> skuSaleDtoList);
}
