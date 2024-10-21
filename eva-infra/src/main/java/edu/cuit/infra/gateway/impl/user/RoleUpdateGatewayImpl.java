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
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        SysRoleDO tmp = checkRoleId(Math.toIntExact(cmd.getId()));
        SysRoleDO roleDO = roleConverter.toRoleDO(cmd);
        roleMapper.updateById(roleDO);
        LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")的信息");
    }

    @Override
    public void updateRoleStatus(Integer roleId, Integer status) {
        SysRoleDO tmp = checkRoleId(roleId);
        LambdaUpdateWrapper<SysRoleDO> roleUpdate = Wrappers.lambdaUpdate();
        roleUpdate.set(SysRoleDO::getStatus,status).eq(SysRoleDO::getId,roleId);
        roleMapper.update(roleUpdate);
        LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的状态");
    }

    @Override
    public void deleteRole(Integer roleId) {
        SysRoleDO tmp = checkRoleId(roleId);
        roleMapper.deleteById(roleId);
        userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getRoleId,roleId));
        roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getRoleId,roleId));

        LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")");
    }

    @Override
    public void deleteMultipleRole(List<Integer> ids) {
        List<SysRoleDO> tmp = new ArrayList<>();
        for (Integer id : ids) {
            tmp.add(checkRoleId(id));
        }
        for (Integer id : ids) {
            roleMapper.deleteById(id);
            userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getRoleId,id));
            roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getRoleId,id));
        }

        LogUtils.logContent(tmp + " 角色");
    }

    @Override
    public void assignPerms(Integer roleId, List<Integer> menuIds) {
        //删除原来的
        SysRoleDO tmp = checkRoleId(roleId);
        LambdaUpdateWrapper<SysRoleMenuDO> roleMenuUpdate = Wrappers.lambdaUpdate();
        roleMenuUpdate.eq(SysRoleMenuDO::getRoleId,roleId);
        roleMenuMapper.delete(roleMenuUpdate);

        //插入新的
        for (Integer id : menuIds) {
            roleMenuMapper.insert(new SysRoleMenuDO()
                    .setMenuId(id)
                    .setRoleId(roleId));
        }

        LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的权限");
    }

    @Override
    public void createRole(NewRoleCmd cmd) {
        SysRoleDO roleDO = roleConverter.toRoleDO(cmd);
        roleMapper.insert(roleDO);
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
