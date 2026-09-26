package io.github.weakll.mall.manager.service;

import io.github.weakll.mall.model.dto.system.AssginRoleDto;
import io.github.weakll.mall.model.dto.system.LoginDto;
import io.github.weakll.mall.model.dto.system.SysUserDto;
import io.github.weakll.mall.model.entity.system.SysUser;
import io.github.weakll.mall.model.vo.system.LoginVo;
import com.github.pagehelper.PageInfo;

public interface SysUserService {
    LoginVo login(LoginDto loginDto);


    SysUser getUserInfo(String token);

    void logout(String token);

    PageInfo<SysUser> findByPage(SysUserDto sysUserDto, Integer pageNum, Integer pageSize);

    void saveSysUser(SysUser sysUser);

    void updateSysUser(SysUser sysUser);

    void deleteById(Long userId);

    void doAssign(AssginRoleDto assginRoleDto);
}
