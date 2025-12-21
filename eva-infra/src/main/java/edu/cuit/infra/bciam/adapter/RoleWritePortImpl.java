package edu.cuit.infra.bciam.adapter;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.port.RoleBatchDeletionPort;
import edu.cuit.bc.iam.application.port.RolePermissionAssignmentPort;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMenuMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：角色写侧端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 *
 * <p>当前收敛范围：角色权限分配、角色批量删除（含缓存失效与日志顺序）。</p>
 */
@Component
@RequiredArgsConstructor
public class RoleWritePortImpl implements RolePermissionAssignmentPort, RoleBatchDeletionPort {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final RoleQueryGateway roleQueryGateway;
    private final UserBasicQueryPort userBasicQueryPort;

    private final LocalCacheManager cacheManager;
    private final UserCacheConstants userCacheConstants;

    @Override
    public void assignPerms(Integer roleId, List<Integer> menuIds) {
        //删除原来的
        SysRoleDO tmp = checkRoleId(roleId);
        LambdaUpdateWrapper<SysRoleMenuDO> roleMenuUpdate = Wrappers.lambdaUpdate();
        roleMenuUpdate.eq(SysRoleMenuDO::getRoleId, roleId);
        roleMenuMapper.delete(roleMenuUpdate);

        //插入新的
        for (Integer id : menuIds) {
            roleMenuMapper.insert(new SysRoleMenuDO().setMenuId(id).setRoleId(roleId));
        }
        handleRoleUpdateCache(roleId);
        LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的权限");
    }

    @Override
    public void deleteMultipleRole(List<Integer> roleIds) {
        List<SysRoleDO> tmp = new ArrayList<>();
        for (Integer id : roleIds) {
            checkDefaultRole(id);
            SysRoleDO roleTmp = checkRoleId(id);
            tmp.add(roleTmp);
            handleRoleUpdateCache(roleTmp.getId());
        }
        for (Integer id : roleIds) {
            roleMapper.deleteById(id);
            userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getRoleId, id));
            roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getRoleId, id));
        }
        LogUtils.logContent(tmp + " 角色");
    }

    private void handleRoleUpdateCache(Integer roleId) {
        cacheManager.invalidateCache(null, userCacheConstants.ALL_ROLE);
        cacheManager.invalidateCache(userCacheConstants.ONE_ROLE, String.valueOf(roleId));
        cacheManager.invalidateCache(userCacheConstants.ROLE_MENU, String.valueOf(roleId));
        LambdaQueryWrapper<SysUserRoleDO> userRoleQuery = Wrappers.lambdaQuery();
        userRoleQuery.eq(SysUserRoleDO::getRoleId, roleId);
        userRoleQuery.select(SysUserRoleDO::getUserId);
        userRoleMapper.selectList(userRoleQuery).forEach(userRole -> {
            cacheManager.invalidateCache(userCacheConstants.ONE_USER_ID, String.valueOf(userRole.getUserId()));
            cacheManager.invalidateCache(
                    userCacheConstants.ONE_USER_USERNAME,
                    userBasicQueryPort.findUsernameById(userRole.getUserId()).orElse(null));
        });
    }

    private void checkDefaultRole(Integer roleId) {
        if (Objects.equals(roleQueryGateway.getDefaultRoleId(), roleId)) {
            throw new BizException("默认角色不允许此操作");
        }
    }

    private SysRoleDO checkRoleId(Integer id) {
        LambdaQueryWrapper<SysRoleDO> roleQuery = Wrappers.lambdaQuery();
        roleQuery.select(SysRoleDO::getId, SysRoleDO::getRoleName).eq(SysRoleDO::getId, id);
        SysRoleDO sysRoleDO = roleMapper.selectOne(roleQuery);
        if (sysRoleDO == null) {
            throw new BizException("角色id: " + id + " 不存在");
        }
        return sysRoleDO;
    }
}

