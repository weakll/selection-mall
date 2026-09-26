package io.github.weakll.mall.manager.service.impl;

import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.manager.helper.MenuHelper;
import io.github.weakll.mall.manager.mapper.SysMenuMapper;
import io.github.weakll.mall.manager.service.SysMenuService;
import io.github.weakll.mall.model.entity.system.SysMenu;
import io.github.weakll.mall.model.entity.system.SysUser;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.system.SysMenuVo;
import io.github.weakll.mall.utils.AuthContextUtil;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
@Service
public class SysMenuServiceImpl implements SysMenuService {
    @Autowired
    private SysMenuMapper sysMenuMapper ;
    @Override
    public List<SysMenu> findNodes() {
        List<SysMenu> sysMenuList = sysMenuMapper.selectAll() ;
        if (CollectionUtils.isEmpty(sysMenuList)) return null;
        List<SysMenu> treeList = MenuHelper.buildTree(sysMenuList); //构建树形数据
        return treeList;
    }
//添加
    @Override
    public void save(SysMenu sysMenu) {
        sysMenuMapper.save(sysMenu) ;
    }

    @Override
    public void updateById(SysMenu sysMenu) {
        sysMenuMapper.updateById(sysMenu) ;
    }

    @Override
    public void removeById(Long id) {
        int count = sysMenuMapper.countByParentId(id); // 先查询是否存在⼦菜单，如果存在不允许进⾏删除
        if (count > 0) {
            throw new MallException(ResultCodeEnum.NODE_ERROR);
        }
        sysMenuMapper.deleteById(id); // 不存在⼦菜单直接删除
    }

    @Override
    public List<SysMenuVo> findUserMenuList() {
        SysUser sysUser = AuthContextUtil.get();
        Long userId = sysUser.getId(); // 获取当前登录⽤⼾的id

        List<SysMenu> sysMenuList = sysMenuMapper.selectListByUserId(userId) ;

        //构建树形数据
        List<SysMenu> sysMenuTreeList = MenuHelper.buildTree(sysMenuList);
        return this.buildMenus(sysMenuTreeList);
    }
    private List<SysMenuVo> buildMenus(List<SysMenu> menus) {

        List<SysMenuVo> sysMenuVoList = new LinkedList<SysMenuVo>();
        for (SysMenu sysMenu : menus) {
            SysMenuVo sysMenuVo = new SysMenuVo();
            sysMenuVo.setTitle(sysMenu.getTitle());
            sysMenuVo.setName(sysMenu.getComponent());
            List<SysMenu> children = sysMenu.getChildren();
            if (!CollectionUtils.isEmpty(children)) {
                sysMenuVo.setChildren(buildMenus(children));
            }
            sysMenuVoList.add(sysMenuVo);
        }
        return sysMenuVoList;
    }
}
