package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.dto.system.SysUserDto;
import io.github.weakll.mall.model.entity.system.SysUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysUserMapper {

    SysUser selectByUserName(String userName);

    List<SysUser> findByPage(SysUserDto sysUserDto);

    SysUser findByUserName(String userName);

    void saveSysUser(SysUser sysUser);

    void updateSysUser(SysUser sysUser);

    void deleteById(Long userId);
}
