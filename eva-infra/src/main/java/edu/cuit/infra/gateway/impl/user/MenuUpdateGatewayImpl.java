package edu.cuit.infra.gateway.impl.user;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.user.MenuConvertor;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysMenuMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMenuMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.infra.enums.cache.UserCacheConstants;
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
    private final SysUserRoleMapper userRoleMapper;

    private final MenuQueryGateway menuQueryGateway;
    private final UserQueryGateway userQueryGateway;

    private final MenuConvertor menuConvertor;

    private final LocalCacheManager localCacheManager;
    private final UserCacheConstants userCacheConstants;

    @Override
    @LocalCacheInvalidateContainer({
            @LocalCacheInvalidate(area = "#{@userCacheConstants.ONE_MENU}",key = "#cmd.id"),
            @LocalCacheInvalidate(area = "#{@userCacheConstants.MENU_CHILDREN}",key = "#cmd.parentId")
    })
    public void updateMenuInfo(UpdateMenuCmd cmd) {
        SysMenuDO tmp = checkMenuId(cmd.getId());
        SysMenuDO menuDO = menuConvertor.toMenuDO(cmd);

        if (tmp.getParentId() != null && !tmp.getParentId().equals(cmd.getParentId())) {
            localCacheManager.invalidateCache(userCacheConstants.MENU_CHILDREN, String.valueOf(tmp.getParentId()));
        }
        menuMapper.updateById(menuDO);

        handleUserMenuCache(cmd.getId());
        LogUtils.logContent(tmp.getName() + " 权限的信息");
    }

    @Override
    public void deleteMenu(Integer menuId) {
        SysMenuDO tmp = checkMenuId(menuId);
        roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getMenuId,menuId));
        deleteMenuAndChildren(menuId);

        handleUserMenuCache(menuId);
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
    @LocalCacheInvalidateContainer({
            @LocalCacheInvalidate(area = "#{@userCacheConstants.MENU_CHILDREN}", key = "#cmd.parentId"),
            @LocalCacheInvalidate(area = "#{@userCacheConstants.ONE_MENU}", key = "#cmd.parentId")
    })
    public void createMenu(NewMenuCmd cmd) {
        if (cmd.getParentId() != null && cmd.getParentId() != 0 && menuQueryGateway.getOne(cmd.getParentId()).isEmpty())
            throw new BizException("父菜单ID: " + cmd.getParentId() + " 不存在");
        SysMenuDO menuDO = menuConvertor.toMenuDO(cmd);
        menuMapper.insert(menuDO);
        localCacheManager.invalidateCache(null,userCacheConstants.ALL_MENU);
    }

    private void handleUserMenuCache(Integer menuId) {
        localCacheManager.invalidateCache(null,userCacheConstants.ALL_MENU);

        LambdaQueryWrapper<SysRoleMenuDO> roleQuery = Wrappers.lambdaQuery();
        roleQuery.eq(SysRoleMenuDO::getMenuId,menuId)
                .select(SysRoleMenuDO::getRoleId);
        roleMenuMapper.selectList(roleQuery).forEach(roleMenu -> {
            localCacheManager.invalidateCache(userCacheConstants.ONE_ROLE, String.valueOf(roleMenu.getRoleId()));

            LambdaQueryWrapper<SysUserRoleDO> userRoleQuery = Wrappers.lambdaQuery();
            userRoleQuery.eq(SysUserRoleDO::getRoleId,roleMenu.getRoleId())
                    .select(SysUserRoleDO::getUserId);
            userRoleMapper.selectList(userRoleQuery).forEach(userRole -> {
                localCacheManager.invalidateCache(userCacheConstants.ONE_USER_ID ,String.valueOf(userRole.getUserId()));
                localCacheManager.invalidateCache(userCacheConstants.ONE_USER_USERNAME,userQueryGateway.findUsernameById(userRole.getUserId()).orElse(null));
            });
        });
    }

    private void deleteMenuAndChildren(Integer menuId) {
        List<MenuEntity> childrenMenus = menuQueryGateway.getChildrenMenus(menuId);
        for (MenuEntity childMenu : childrenMenus) {
            deleteMenuAndChildren(childMenu.getId());
        }
        SysMenuDO tmp = checkMenuId(menuId);

        menuMapper.deleteById(menuId);
        localCacheManager.invalidateCache(userCacheConstants.MENU_CHILDREN , String.valueOf(tmp.getParentId()));
        localCacheManager.invalidateCache(userCacheConstants.ONE_MENU, String.valueOf(tmp.getId()));
        handleUserMenuCache(menuId);
        deleteRoleMenu(menuId);
    }

    private void deleteRoleMenu(Integer menuId) {
        LambdaQueryWrapper<SysRoleMenuDO> roleMenuQuery = Wrappers.lambdaQuery();
        roleMenuQuery.eq(SysRoleMenuDO::getMenuId,menuId);
        List<SysRoleMenuDO> sysRoleMenuList = roleMenuMapper.selectList(roleMenuQuery);
        roleMenuMapper.delete(roleMenuQuery);

        for (SysRoleMenuDO sysRoleMenuDO : sysRoleMenuList) {
            localCacheManager.invalidateCache(userCacheConstants.ROLE_MENU , String.valueOf(sysRoleMenuDO.getRoleId()));
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
