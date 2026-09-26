package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.dto.system.SysRoleDto;
import io.github.weakll.mall.model.entity.system.SysRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysRoleMapper {
    List<SysRole> findByPage(SysRoleDto sysRoleDto);

    void saveSysRole(SysRole sysRole);

    void deleteById(Long roleId);

    void updateSysRole(SysRole sysRole);

    List<SysRole> findAllRoles();
}
