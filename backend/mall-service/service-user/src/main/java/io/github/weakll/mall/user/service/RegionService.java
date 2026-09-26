package io.github.weakll.mall.user.service;

import io.github.weakll.mall.model.entity.base.Region;

import java.util.List;

public interface RegionService {
    List<Region> findByParentCode(Long parentCode);
}
