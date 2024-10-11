package edu.cuit.infra.gateway.impl.user;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.client.dto.cmd.user.NewRoleCmd;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.domain.gateway.user.RoleUpdateGateway;
import edu.cuit.infra.convertor.user.RoleConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMenuMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class RoleUpdateGatewayImpl implements RoleUpdateGateway {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final RoleConverter roleConverter;

    @Override
    public void updateRoleInfo(UpdateRoleCmd cmd) {
        checkRoleId(Math.toIntExact(cmd.getId()));
        SysRoleDO roleDO = roleConverter.toRoleDO(cmd);
        roleMapper.updateById(roleDO);
        //TODO app层处理状态更新
    }

    @Override
    public void updateRoleStatus(Integer roleId, Integer status) {
        checkRoleId(roleId);
        LambdaUpdateWrapper<SysRoleDO> roleUpdate = Wrappers.lambdaUpdate();
        roleUpdate.set(SysRoleDO::getStatus,status).eq(SysRoleDO::getId,roleId);
        roleMapper.update(roleUpdate);
    }

    @Override
    public void deleteRole(Integer roleId) {
        checkRoleId(roleId);
        roleMapper.deleteById(roleId);
        userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getRoleId,roleId));
        roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getRoleId,roleId));
    }

    @Override
    public void deleteMultipleRole(List<Integer> ids) {
        for (Integer id : ids) {
            checkRoleId(id);
        }
        for (Integer id : ids) {
            roleMapper.deleteById(id);
            userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getRoleId,id));
            roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getRoleId,id));
        }
    }

    @Override
    public void assignPerms(Integer roleId, List<Integer> menuIds) {
        //删除原来的
        checkRoleId(roleId);
        LambdaUpdateWrapper<SysRoleMenuDO> roleMenuUpdate = Wrappers.lambdaUpdate();
        roleMenuUpdate.eq(SysRoleMenuDO::getRoleId,roleId);
        roleMenuMapper.delete(roleMenuUpdate);

        //插入新的
        for (Integer id : menuIds) {
            roleMenuMapper.insert(new SysRoleMenuDO()
                    .setMenuId(id)
                    .setRoleId(roleId));
        }
    }

    @Override
    public void createRole(NewRoleCmd cmd) {
        SysRoleDO roleDO = roleConverter.toRoleDO(cmd);
        roleMapper.insert(roleDO);
    }

    private void checkRoleId(Integer id) {
        LambdaQueryWrapper<SysRoleDO> roleQuery = Wrappers.lambdaQuery();
        roleQuery.select(SysRoleDO::getId).eq(SysRoleDO::getId,id);
        SysRoleDO sysRoleDO = roleMapper.selectOne(roleQuery);
        if (sysRoleDO == null) {
            throw new BizException("角色id: " + id + " 不存在");
        }
    }
}
