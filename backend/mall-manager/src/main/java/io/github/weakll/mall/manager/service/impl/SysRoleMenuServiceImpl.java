package io.github.weakll.mall.manager.service.impl;

import io.github.weakll.mall.manager.mapper.SysRoleMenuMapper;
import io.github.weakll.mall.manager.service.SysMenuService;
import io.github.weakll.mall.manager.service.SysRoleMenuService;
import io.github.weakll.mall.model.dto.system.AssginMenuDto;
import io.github.weakll.mall.model.entity.system.SysMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class SysRoleMenuServiceImpl implements SysRoleMenuService {
    @Autowired
    private SysMenuService sysMenuService;
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Override
    public Map<String, Object> findSysRoleMenuByRoleId(Long roleId) {
        // 查询所有的菜单数据
        List<SysMenu> sysMenuList = sysMenuService.findNodes();
        // 查询当前⻆⾊的菜单数据
        List<Long> roleMenuIds = sysRoleMenuMapper.findSysRoleMenuByRoleId(roleId) ;
        // 将数据存储到Map中进⾏返回
        Map<String , Object> result = new HashMap<>() ;
        result.put("sysMenuList" , sysMenuList) ;
        result.put("roleMenuIds" , roleMenuIds) ;
        // 返回
        return result;
    }
    @Transactional
        @Override
        public void doAssign(AssginMenuDto assginMenuDto) {

        // 根据⻆⾊的id删除其所对应的菜单数据
        sysRoleMenuMapper.deleteByRoleId(assginMenuDto.getRoleId());

        // 获取菜单的id
        List<Map<String, Number>> menuInfo = assginMenuDto.getMenuIdList();
        if (menuInfo != null && menuInfo.size() > 0) {
            sysRoleMenuMapper.doAssign(assginMenuDto);
        }

    }
}
