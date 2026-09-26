package io.github.weakll.mall.manager.service.impl;

import io.github.weakll.mall.manager.mapper.ProductUnitMapper;
import io.github.weakll.mall.manager.service.ProductUnitService;
import io.github.weakll.mall.model.entity.base.ProductUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ProductUnitServiceImpl implements ProductUnitService {
    @Autowired
    private ProductUnitMapper productUnitMapper;
    @Override
    public List<ProductUnit> findAll() {
        return productUnitMapper.findAll();
    }
}
