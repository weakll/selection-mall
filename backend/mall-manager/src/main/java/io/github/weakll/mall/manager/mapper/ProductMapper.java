package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.dto.product.ProductDto;
import io.github.weakll.mall.model.entity.product.Product;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface ProductMapper {
    List<Product> findByPage(ProductDto productDto);

    void save(Product product);

    Product selectById(Long id);

    void updateById(Product product);

    void deleteById(Long id);
}
