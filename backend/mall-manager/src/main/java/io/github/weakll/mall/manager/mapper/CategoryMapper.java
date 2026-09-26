package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.entity.product.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<Category> selectByParentId(Long parentId);

    int countByParentId(Long id);

    List<Category> selectAll();

    void batchInsert(List cachedDataList);

    List<Category> findOneCategory();
}
