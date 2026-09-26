package io.github.weakll.mall.product.service.impl;

import com.alibaba.fastjson.JSON;
import io.github.weakll.mall.model.entity.product.Category;
import io.github.weakll.mall.product.mapper.CategoryMapper;
import io.github.weakll.mall.product.service.CategoryService;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {
    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private RedisTemplate<String , String> redisTemplate ;
    @Override
    public List<Category> findOneCategory() {
        // 从Redis缓存中查询所有的一级分类数据
        String categoryListJSON = redisTemplate.opsForValue().get("category:one");
        if(!StringUtils.isEmpty(categoryListJSON)) {
            List<Category> categoryList = JSON.parseArray(categoryListJSON, Category.class);
            log.info("从Redis缓存中查询到了所有的一级分类数据");
            return categoryList ;
        }
        List<Category> categoryList = categoryMapper.findOneCategory();
        log.info("从数据库中查询到了所有的一级分类数据");
        redisTemplate.opsForValue().set("category:one" , JSON.toJSONString(categoryList) , 7 , TimeUnit.DAYS);
        return categoryList ;
    }


    @Override
    @Cacheable(value = "category" , key = "'all'")
    public List<Category> findOneCategoryTree() {
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
