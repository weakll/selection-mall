package io.github.weakll.mall.product.service;

import io.github.weakll.mall.model.entity.product.Category;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface CategoryService {
    List<Category> findOneCategory();

    List<Category> findOneCategoryTree();

    List<Category> findCategoryTree();
}
