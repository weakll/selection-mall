package io.github.weakll.mall.manager.service;

import io.github.weakll.mall.model.dto.product.ProductDto;
import io.github.weakll.mall.model.entity.product.Product;
import io.github.weakll.mall.model.entity.product.ProductSku;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface ProductService {
    PageInfo<Product> findByPage(Integer page, Integer limit, ProductDto productDto);

    void save(Product product);

    Product getById(Long id);

    void updateById(Product product);

    void deleteById(Long id);

    void updateAuditStatus(Long id, Integer auditStatus);

    void updateStatus(Long id, Integer status);

}
