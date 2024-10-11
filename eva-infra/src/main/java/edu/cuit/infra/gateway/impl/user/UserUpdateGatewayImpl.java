package edu.cuit.infra.gateway.impl.user;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.domain.gateway.user.UserUpdateGateway;
import edu.cuit.infra.convertor.user.LdapUserConvertor;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.infra.dal.ldap.dataobject.LdapPersonDO;
import edu.cuit.infra.dal.ldap.repo.LdapPersonRepo;
import edu.cuit.infra.util.EvaLdapUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class UserUpdateGatewayImpl implements UserUpdateGateway {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final LdapPersonRepo ldapPersonRepo;

    private final UserConverter userConverter;
    private final LdapUserConvertor ldapUserConvertor;

    @Override
    public void updateInfo(UpdateUserCmd cmd) {
        checkIdExistence(Math.toIntExact(cmd.getId()));
        SysUserDO userDO = userConverter.toUserDO(cmd);
        if (userDO.getUsername().equals(cmd.getUsername())) {
            throw new BizException("用户名不能与原来相同");
        }
        if (checkUsernameExistence(cmd.getUsername())) {
            throw new BizException("用户名已存在");
        }
        LdapPersonDO personDO = ldapUserConvertor.userDOToLdapPersonDO(userDO);
        userMapper.updateById(userDO);
        ldapPersonRepo.save(personDO);
        //TODO 在app层处理status更改逻辑
    }

    @Override
    public void updateStatus(Integer userId, Integer status) {
        checkIdExistence(userId);
        LambdaUpdateWrapper<SysUserDO> userUpdate = Wrappers.lambdaUpdate();
        userUpdate.set(SysUserDO::getStatus,status).eq(SysUserDO::getId,userUpdate);
        userMapper.update(userUpdate);
    }

    @Override
    public void deleteUser(Integer userId) {
        checkIdExistence(userId);
        userMapper.deleteById(userId);
        ldapPersonRepo.deleteById(EvaLdapUtils.getUserLdapNameId(getUsername(userId)));
        userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getUserId,userId));
    }

    @Override
    public void assignRole(Integer userId, List<Integer> roleId) {
        //删除原来的
        checkIdExistence(userId);
        LambdaUpdateWrapper<SysUserRoleDO> userRoleUpdate = Wrappers.lambdaUpdate();
        userRoleUpdate.eq(SysUserRoleDO::getUserId,userId);
        userRoleMapper.delete(userRoleUpdate);

        //插入新的
        for (Integer id : roleId) {
            userRoleMapper.insert(new SysUserRoleDO()
                    .setUserId(userId)
                    .setRoleId(id));
        }
    }

    @Override
    public void createUser(NewUserCmd cmd) {
        if (checkUsernameExistence(cmd.getUsername())) {
            throw new BizException("用户名已存在");
        }
        SysUserDO userDO = userConverter.toUserDO(cmd);
        LdapPersonDO personDO = ldapUserConvertor.userDOToLdapPersonDO(userDO);
        userMapper.insert(userDO);
        ldapPersonRepo.save(personDO);
    }

    /**
     * 检查id是否存在
     */
    private void checkIdExistence(Integer id) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.lambdaQuery();
        query.select(SysUserDO::getId).eq(SysUserDO::getId,id);
        if (userMapper.selectOne(query) == null) {
            throw new BizException("用户id不存在");
        }
    }

    /**
     * 检查用户名是否存在
     */
    private boolean checkUsernameExistence(String username) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.lambdaQuery();
        query.select(SysUserDO::getUsername).eq(SysUserDO::getUsername,username);
        return userMapper.selectOne(query) != null;
    }

    private String getUsername(Integer id) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.lambdaQuery();
        query.select(SysUserDO::getUsername).eq(SysUserDO::getId,id);
        SysUserDO sysUserDO = userMapper.selectOne(query);
        if (sysUserDO != null) {
            return sysUserDO.getUsername();
        } else return null;
    }
}
