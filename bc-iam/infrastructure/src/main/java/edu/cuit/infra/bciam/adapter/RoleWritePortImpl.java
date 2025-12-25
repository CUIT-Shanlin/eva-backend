package edu.cuit.infra.bciam.adapter;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.port.RoleBatchDeletionPort;
import edu.cuit.bc.iam.application.port.RoleCreationPort;
import edu.cuit.bc.iam.application.port.RoleDeletionPort;
import edu.cuit.bc.iam.application.port.RoleInfoUpdatePort;
import edu.cuit.bc.iam.application.port.RolePermissionAssignmentPort;
import edu.cuit.bc.iam.application.port.RoleStatusUpdatePort;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.client.dto.cmd.user.NewRoleCmd;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.infra.convertor.user.RoleConverter;
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
 * <p>当前收敛范围：角色权限分配、角色批量删除、角色信息/状态更新、角色删除、角色创建（含缓存失效与日志顺序）。</p>
 */
@Component
@RequiredArgsConstructor
public class RoleWritePortImpl
        implements RolePermissionAssignmentPort,
                RoleBatchDeletionPort,
                RoleInfoUpdatePort,
                RoleStatusUpdatePort,
                RoleDeletionPort,
                RoleCreationPort {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final RoleQueryGateway roleQueryGateway;
    private final UserBasicQueryPort userBasicQueryPort;

    private final RoleConverter roleConverter;

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

    @Override
    public void updateRoleInfo(UpdateRoleCmd cmd) {
        SysRoleDO tmp = checkRoleId(Math.toIntExact(cmd.getId()));
        if (cmd.getStatus() == 0) checkDefaultRole(Math.toIntExact(cmd.getId()));
        SysRoleDO roleDO = roleConverter.toRoleDO(cmd);
        roleMapper.updateById(roleDO);
        handleRoleUpdateCache(Math.toIntExact(cmd.getId()));
        LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")的信息");
    }

    @Override
    public void updateRoleStatus(Integer roleId, Integer status) {
        SysRoleDO tmp = checkRoleId(roleId);
        checkDefaultRole(roleId);
        LambdaUpdateWrapper<SysRoleDO> roleUpdate = Wrappers.lambdaUpdate();
        roleUpdate.set(SysRoleDO::getStatus, status).eq(SysRoleDO::getId, roleId);
        roleMapper.update(roleUpdate);
        handleRoleUpdateCache(roleId);
        LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的状态");
    }

    @Override
    public void deleteRole(Integer roleId) {
        SysRoleDO tmp = checkRoleId(roleId);
        checkDefaultRole(roleId);
        roleMapper.deleteById(roleId);
        userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getRoleId, roleId));
        roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getRoleId, roleId));

        handleRoleUpdateCache(roleId);

        LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")");
    }

    @Override
    public void createRole(NewRoleCmd cmd) {
        SysRoleDO roleDO = roleConverter.toRoleDO(cmd);
        if (isRoleNameExisted(cmd.getRoleName())) throw new BizException("角色名称已存在");
        roleMapper.insert(roleDO);
        handleRoleUpdateCache(roleDO.getId());
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

    private boolean isRoleNameExisted(String roleName) {
        LambdaQueryWrapper<SysRoleDO> roleQuery = Wrappers.lambdaQuery();
        roleQuery.select(SysRoleDO::getId).eq(SysRoleDO::getRoleName, roleName);
        return roleMapper.selectOne(roleQuery) != null;
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

