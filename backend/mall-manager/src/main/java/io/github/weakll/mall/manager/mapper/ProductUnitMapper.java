package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.entity.base.ProductUnit;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductUnitMapper {
    List<ProductUnit> findAll();
}
