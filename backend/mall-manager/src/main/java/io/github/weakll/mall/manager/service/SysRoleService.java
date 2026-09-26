package io.github.weakll.mall.manager.service;

import io.github.weakll.mall.model.dto.system.SysRoleDto;
import io.github.weakll.mall.model.entity.system.SysRole;
import com.github.pagehelper.PageInfo;

import java.util.Map;

public interface SysRoleService {


     PageInfo<SysRole> findByPage(SysRoleDto sysRoleDto, Integer pageNum, Integer pageSize);

    void saveSysRole(SysRole sysRole);

    void updateSysRole(SysRole sysRole);

    void deleteById(Long roleId);


    Map<String, Object> findAllRoles(Long userId);
}
