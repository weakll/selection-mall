package io.github.weakll.mall.user.mapper;

import io.github.weakll.mall.model.entity.base.Region;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RegionMapper {
    List<Region> findByParentCode(Long parentCode);
}
