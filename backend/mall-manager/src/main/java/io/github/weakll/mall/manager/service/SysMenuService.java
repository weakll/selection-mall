package io.github.weakll.mall.manager.service;

import io.github.weakll.mall.model.entity.system.SysMenu;
import io.github.weakll.mall.model.vo.system.SysMenuVo;

import java.util.List;

public interface SysMenuService {
    List<SysMenu> findNodes();

    void save(SysMenu sysMenu);

    void updateById(SysMenu sysMenu);

    void removeById(Long id);

    List<SysMenuVo> findUserMenuList();
}
