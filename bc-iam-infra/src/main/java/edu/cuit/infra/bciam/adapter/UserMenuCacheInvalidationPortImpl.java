package edu.cuit.infra.bciam.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.bc.iam.application.port.UserMenuCacheInvalidationPort;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMenuMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：菜单变更触发的用户菜单缓存失效端口适配器（保持历史行为不变：原样搬运旧实现）。
 */
@Component
@RequiredArgsConstructor
public class UserMenuCacheInvalidationPortImpl implements UserMenuCacheInvalidationPort {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final LocalCacheManager localCacheManager;
    private final UserCacheConstants userCacheConstants;
    private final UserBasicQueryPort userBasicQueryPort;

    @Override
    public void handleUserMenuCache(Integer menuId) {
        localCacheManager.invalidateCache(null, userCacheConstants.ALL_MENU);

        LambdaQueryWrapper<SysRoleMenuDO> roleQuery = Wrappers.lambdaQuery();
        roleQuery.eq(SysRoleMenuDO::getMenuId, menuId).select(SysRoleMenuDO::getRoleId);
        roleMenuMapper.selectList(roleQuery).forEach(roleMenu -> {
            localCacheManager.invalidateCache(userCacheConstants.ONE_ROLE, String.valueOf(roleMenu.getRoleId()));

            LambdaQueryWrapper<SysUserRoleDO> userRoleQuery = Wrappers.lambdaQuery();
            userRoleQuery.eq(SysUserRoleDO::getRoleId, roleMenu.getRoleId()).select(SysUserRoleDO::getUserId);
            userRoleMapper.selectList(userRoleQuery).forEach(userRole -> {
                localCacheManager.invalidateCache(userCacheConstants.ONE_USER_ID, String.valueOf(userRole.getUserId()));
                localCacheManager.invalidateCache(
                        userCacheConstants.ONE_USER_USERNAME,
                        userBasicQueryPort.findUsernameById(userRole.getUserId()).orElse(null));
            });
        });
    }
}

