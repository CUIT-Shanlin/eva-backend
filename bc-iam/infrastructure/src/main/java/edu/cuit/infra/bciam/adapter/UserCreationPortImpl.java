package edu.cuit.infra.bciam.adapter;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.port.UserCreationPort;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewUserCmd;
import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.infra.convertor.user.LdapUserConvertor;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：用户创建端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class UserCreationPortImpl implements UserCreationPort {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final RoleQueryGateway roleQueryGateway;
    private final UserConverter userConverter;
    private final LdapUserConvertor ldapUserConvertor;
    private final LdapPersonGateway ldapPersonGateway;
    private final LocalCacheManager cacheManager;
    private final UserCacheConstants userCacheConstants;
    private final CourseCacheConstants courseCacheConstants;

    @Override
    public void createUser(NewUserCmd cmd) {
        if (checkUsernameExistence(cmd.getUsername())) {
            throw new BizException("用户名已存在");
        }

        SysUserDO existedUsername = userMapper.findIdByUsername(cmd.getUsername());
        if (existedUsername != null) {
            throw new BizException("该用户名已存在于归档的用户（数据库逻辑删除）中");
        }

        SysUserDO userDO = userConverter.toUserDO(cmd);
        LdapPersonEntity ldapPerson = ldapUserConvertor.userDOToLdapPersonEntity(userDO);
        userMapper.insert(userDO);
        SysUserRoleDO sysUserRoleDO = new SysUserRoleDO()
                .setRoleId(roleQueryGateway.getDefaultRoleId())
                .setUserId(userDO.getId());
        userRoleMapper.insert(sysUserRoleDO);
        if (ldapPersonGateway.findByUsername(cmd.getUsername()).isEmpty()) {
            ldapPersonGateway.createUser(ldapPerson, cmd.getPassword());
        }

        handleUserUpdateCache(userDO.getId(), cmd.getUsername());
    }

    private boolean checkUsernameExistence(String username) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.lambdaQuery();
        query.select(SysUserDO::getUsername).eq(SysUserDO::getUsername, username);
        return userMapper.selectOne(query) != null;
    }

    private void handleUserUpdateCache(Integer userId, String username) {
        cacheManager.invalidateCache(userCacheConstants.ONE_USER_ID, String.valueOf(userId));
        cacheManager.invalidateCache(userCacheConstants.ONE_USER_USERNAME, username);
        cacheManager.invalidateCache(null, userCacheConstants.ALL_USER);
        cacheManager.invalidateCache(null, userCacheConstants.ALL_USER_USERNAME);
        cacheManager.invalidateCache(userCacheConstants.USER_ROLE, String.valueOf(userId));
        cacheManager.invalidateCache(null, userCacheConstants.ALL_DEPARTMENT);
        cacheManager.invalidateArea(courseCacheConstants.COURSE_LIST_BY_SEM);
    }
}

