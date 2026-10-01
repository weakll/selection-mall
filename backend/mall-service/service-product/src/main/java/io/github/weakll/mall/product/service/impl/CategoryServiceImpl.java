package io.github.weakll.mall.product.service.impl;

import io.github.weakll.mall.common.cache.CacheConstants;
import io.github.weakll.mall.model.entity.product.Category;
import io.github.weakll.mall.product.mapper.CategoryMapper;
import io.github.weakll.mall.product.service.CategoryService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.time.Duration;
import io.github.weakll.mall.product.cache.CatalogCache;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private CatalogCache catalogCache;

    @Override
    public List<Category> findOneCategory() {
        return catalogCache.getListOrLoad(
                CacheConstants.CATEGORY_ONE,
                "all",
                Duration.ofHours(6),
                Category.class,
                categoryMapper::findOneCategory
        );
    }


    @Override
    public List<Category> findOneCategoryTree() {
        return catalogCache.getListOrLoad(
                CacheConstants.CATEGORY_TREE,
                "all",
                Duration.ofHours(6),
                Category.class,
                this::loadCategoryTree
        );
    }

    private List<Category> loadCategoryTree() {
        List<Category> categoryList = categoryMapper.findAll();
        if (CollectionUtils.isEmpty(categoryList)) {
            return null;
        }

        List<Category> oneCategoryList = categoryList.stream()
                .filter(item -> item.getParentId() != null && item.getParentId().longValue() == 0)
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(oneCategoryList)) {
            oneCategoryList.forEach(oneCategory -> {
                List<Category> twoCategoryList = categoryList.stream()
                        .filter(item -> item.getParentId() != null && item.getParentId().longValue() == oneCategory.getId().longValue())
                        .collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(twoCategoryList)) {
                    twoCategoryList.forEach(twoCategory -> {
                        List<Category> threeCategoryList = categoryList.stream()
                                .filter(item -> item.getParentId() != null && item.getParentId().longValue() == twoCategory.getId().longValue())
                                .collect(Collectors.toList());
                        twoCategory.setChildren(threeCategoryList);
                    });
                    oneCategory.setChildren(twoCategoryList);
                }
            });
        }
        return oneCategoryList;
    }

    @Override
    public List<Category> findCategoryTree() {
        return List.of();
    }
}
