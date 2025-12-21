package edu.cuit.infra.gateway.impl.user;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.usecase.AssignRolePermsUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteMultipleRoleUseCase;
import edu.cuit.client.dto.cmd.user.NewRoleCmd;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.domain.gateway.user.RoleUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class RoleUpdateGatewayImpl implements RoleUpdateGateway {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final RoleQueryGateway roleQueryGateway;
    private final UserQueryGateway userQueryGateway;

    private final RoleConverter roleConverter;

    private final LocalCacheManager cacheManager;
    private final UserCacheConstants userCacheConstants;

    private final AssignRolePermsUseCase assignRolePermsUseCase;
    private final DeleteMultipleRoleUseCase deleteMultipleRoleUseCase;

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
        roleUpdate.set(SysRoleDO::getStatus,status).eq(SysRoleDO::getId,roleId);
        roleMapper.update(roleUpdate);
        handleRoleUpdateCache(roleId);
        LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的状态");
    }

    @Override
    public void deleteRole(Integer roleId) {
        SysRoleDO tmp = checkRoleId(roleId);
        checkDefaultRole(roleId);
        roleMapper.deleteById(roleId);
        userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getRoleId,roleId));
        roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getRoleId,roleId));

        handleRoleUpdateCache(roleId);

        LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")");
    }

    @Override
    public void deleteMultipleRole(List<Integer> ids) {
        deleteMultipleRoleUseCase.execute(ids);
    }

    @Override
    public void assignPerms(Integer roleId, List<Integer> menuIds) {
        assignRolePermsUseCase.execute(roleId, menuIds);
    }

    @Override
    public void createRole(NewRoleCmd cmd) {
        SysRoleDO roleDO = roleConverter.toRoleDO(cmd);
        if (isRoleNameExisted(cmd.getRoleName())) throw new BizException("角色名称已存在");
        roleMapper.insert(roleDO);
        handleRoleUpdateCache(roleDO.getId());
    }

    private void handleRoleUpdateCache(Integer roleId) {
        cacheManager.invalidateCache(null,userCacheConstants.ALL_ROLE);
        cacheManager.invalidateCache(userCacheConstants.ONE_ROLE , String.valueOf(roleId));
        cacheManager.invalidateCache(userCacheConstants.ROLE_MENU , String.valueOf(roleId));
        LambdaQueryWrapper<SysUserRoleDO> userRoleQuery = Wrappers.lambdaQuery();
        userRoleQuery.eq(SysUserRoleDO::getRoleId,roleId);
        userRoleQuery.select(SysUserRoleDO::getUserId);
        userRoleMapper.selectList(userRoleQuery).forEach(userRole -> {
            cacheManager.invalidateCache(userCacheConstants.ONE_USER_ID, String.valueOf(userRole.getUserId()));
            cacheManager.invalidateCache(userCacheConstants.ONE_USER_USERNAME,userQueryGateway.findUsernameById(userRole.getUserId()).orElse(null));
        });
    }

    private boolean isRoleNameExisted(String roleName) {
        LambdaQueryWrapper<SysRoleDO> roleQuery = Wrappers.lambdaQuery();
        roleQuery.select(SysRoleDO::getId)
                .eq(SysRoleDO::getRoleName,roleName);
        return roleMapper.selectOne(roleQuery) != null;
    }

    private void checkDefaultRole(Integer roleId) {
        if (Objects.equals(roleQueryGateway.getDefaultRoleId(), roleId)) {
            throw new BizException("默认角色不允许此操作");
        }
    }

    private SysRoleDO checkRoleId(Integer id) {
        LambdaQueryWrapper<SysRoleDO> roleQuery = Wrappers.lambdaQuery();
        roleQuery.select(SysRoleDO::getId,SysRoleDO::getRoleName).eq(SysRoleDO::getId,id);
        SysRoleDO sysRoleDO = roleMapper.selectOne(roleQuery);
        if (sysRoleDO == null) {
            throw new BizException("角色id: " + id + " 不存在");
        }
        return sysRoleDO;
    }
}
