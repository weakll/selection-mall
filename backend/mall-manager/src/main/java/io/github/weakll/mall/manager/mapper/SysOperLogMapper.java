package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.entity.system.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperLogMapper {
    void insert(SysOperLog sysOperLog);
}
