package io.github.weakll.mall.user.service.impl;

import io.github.weakll.mall.model.entity.base.Region;
import io.github.weakll.mall.user.mapper.RegionMapper;
import io.github.weakll.mall.user.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RegionServiceImpl implements RegionService {

    @Autowired
    private RegionMapper regionMapper;

    @Override
    public List<Region> findByParentCode(Long parentCode) {
        return regionMapper.findByParentCode(parentCode);
    }
}
