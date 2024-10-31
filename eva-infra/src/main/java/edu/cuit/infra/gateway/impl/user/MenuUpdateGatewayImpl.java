package edu.cuit.infra.gateway.impl.user;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import edu.cuit.infra.convertor.user.MenuConvertor;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleMenuDO;
import edu.cuit.infra.dal.database.mapper.user.SysMenuMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMenuMapper;
import edu.cuit.infra.enums.CacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCacheInvalidate;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCacheInvalidateContainer;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class MenuUpdateGatewayImpl implements MenuUpdateGateway {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    private final MenuQueryGateway menuQueryGateway;

    private final MenuConvertor menuConvertor;

    private final LocalCacheManager localCacheManager;
    private final CacheConstants cacheConstants;

    @Override
    @LocalCacheInvalidateContainer({
            @LocalCacheInvalidate(key = "#{@cacheConstants.ONE_MENU + #cmd.id}"),
            @LocalCacheInvalidate(key = "#{@cacheConstants.MENU_CHILDREN + #cmd.parentId}")
    })
    public void updateMenuInfo(UpdateMenuCmd cmd) {
        SysMenuDO tmp = checkMenuId(cmd.getId());
        SysMenuDO menuDO = menuConvertor.toMenuDO(cmd);

        if (tmp.getParentId() != null && !tmp.getParentId().equals(cmd.getParentId())) {
            localCacheManager.invalidateCache(cacheConstants.MENU_CHILDREN + tmp.getParentId());
        }
        menuMapper.updateById(menuDO);

        LogUtils.logContent(tmp.getName() + " 权限的信息");
    }

    @Override
    public void deleteMenu(Integer menuId) {
        SysMenuDO tmp = checkMenuId(menuId);
        roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getMenuId,menuId));
        deleteMenuAndChildren(menuId);

        LogUtils.logContent(tmp.getName() + " 权限");
    }

    @Override
    public void deleteMultipleMenu(List<Integer> menuIds) {
        List<SysMenuDO> tmp = new ArrayList<>();
        for (Integer menuId : menuIds) {
            tmp.add(checkMenuId(menuId));
        }
        for (Integer menuId : menuIds) {
            deleteMenuAndChildren(menuId);
        }

        LogUtils.logContent(tmp + " 权限");
    }

    @Override
    @LocalCacheInvalidate(key = "#{@cacheConstants.MENU_CHILDREN + #cmd.parentId}")
    public void createMenu(NewMenuCmd cmd) {
        if (cmd.getParentId() != null && cmd.getParentId() != 0 && menuQueryGateway.getOne(cmd.getParentId()).isEmpty())
            throw new BizException("父菜单ID: " + cmd.getParentId() + " 不存在");
        SysMenuDO menuDO = menuConvertor.toMenuDO(cmd);
        menuMapper.insert(menuDO);

    }

    private void deleteMenuAndChildren(Integer menuId) {
        List<MenuEntity> childrenMenus = menuQueryGateway.getChildrenMenus(menuId);
        for (MenuEntity childMenu : childrenMenus) {
            deleteMenuAndChildren(childMenu.getId());
        }
        SysMenuDO tmp = checkMenuId(menuId);

        menuMapper.deleteById(menuId);
        localCacheManager.invalidateCache(cacheConstants.MENU_CHILDREN + tmp.getParentId());
        localCacheManager.invalidateCache(cacheConstants.ONE_MENU + tmp.getId());
        deleteRoleMenu(menuId);
    }

    private void deleteRoleMenu(Integer menuId) {
        LambdaQueryWrapper<SysRoleMenuDO> roleMenuQuery = Wrappers.lambdaQuery();
        roleMenuQuery.eq(SysRoleMenuDO::getMenuId,menuId);
        List<SysRoleMenuDO> sysRoleMenuList = roleMenuMapper.selectList(roleMenuQuery);
        roleMenuMapper.delete(roleMenuQuery);

        for (SysRoleMenuDO sysRoleMenuDO : sysRoleMenuList) {
            localCacheManager.invalidateCache(cacheConstants.ROLE_MENU + sysRoleMenuDO.getRoleId());
        }
    }

    private SysMenuDO checkMenuId(Integer id) {
        LambdaQueryWrapper<SysMenuDO> menuQuery = Wrappers.lambdaQuery();
        menuQuery.select(SysMenuDO::getId,SysMenuDO::getName,SysMenuDO::getParentId)
                .eq(SysMenuDO::getId,id);
        SysMenuDO sysMenuDO = menuMapper.selectOne(menuQuery);
        if (sysMenuDO == null) {
            throw new BizException("菜单id: " + id + " 不存在");
        }
        return sysMenuDO;
    }

}
