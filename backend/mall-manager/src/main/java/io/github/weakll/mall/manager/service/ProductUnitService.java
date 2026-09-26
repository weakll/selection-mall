package io.github.weakll.mall.manager.service;

import io.github.weakll.mall.model.entity.base.ProductUnit;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ProductUnitService {
    List<ProductUnit> findAll();
}
