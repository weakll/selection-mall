package io.github.weakll.mall.manager.service;

import io.github.weakll.mall.model.entity.product.ProductSpec;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface ProductSpecService {
    PageInfo<ProductSpec> findByPage(Integer page, Integer limit);

    void save(ProductSpec productSpec);

    void updateById(ProductSpec productSpec);

    void deleteById(Long id);

    List<ProductSpec> findAll();
}
