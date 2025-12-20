package edu.cuit.infra.gateway.impl.user;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.usecase.AssignRoleUseCase;
import edu.cuit.bc.iam.application.usecase.CreateUserUseCase;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.domain.gateway.user.UserUpdateGateway;
import edu.cuit.infra.convertor.user.LdapUserConvertor;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.infra.dal.ldap.dataobject.LdapPersonDO;
import edu.cuit.infra.dal.ldap.repo.LdapPersonRepo;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.infra.util.EvaLdapUtils;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class UserUpdateGatewayImpl implements UserUpdateGateway {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final LdapPersonRepo ldapPersonRepo;

    private final LdapPersonGateway ldapPersonGateway;
    private final RoleQueryGateway roleQueryGateway;

    private final UserConverter userConverter;
    private final LdapUserConvertor ldapUserConvertor;

    private final LocalCacheManager cacheManager;
    private final UserCacheConstants userCacheConstants;
    private final CourseCacheConstants courseCacheConstants;

    private final AssignRoleUseCase assignRoleUseCase;
    private final CreateUserUseCase createUserUseCase;

    @Override
    public void updateInfo(UpdateUserCmd cmd) {
        SysUserDO tmp = checkIdExistence(Math.toIntExact(cmd.getId()));
        SysUserDO userDO = userConverter.toUserDO(cmd);
        String oldUsername = userDO.getUsername();
        checkAdmin(Math.toIntExact(cmd.getId()));
        if (checkUsernameExistence(cmd.getUsername()) && !oldUsername.equals(cmd.getUsername())) {
            throw new BizException("该用户名已存在");
        }
        if (cmd.getStatus() != null && cmd.getStatus() == 0) checkAdmin(Math.toIntExact(cmd.getId()));
        userMapper.updateById(userDO);
        LdapPersonEntity ldapPersonEntity = ldapUserConvertor.userDOToLdapPersonEntity(userDO);
        ldapPersonGateway.saveUser(ldapPersonEntity);

        handleUserUpdateCache(Math.toIntExact(cmd.getId()));

        LogUtils.logContent(tmp.getName() + " 用户(id:" + tmp.getId() + ")的信息");
    }

    @Override
    public void updateStatus(Integer userId, Integer status) {
        SysUserDO tmp = checkIdExistence(userId);
        checkAdmin(userId);
        LambdaUpdateWrapper<SysUserDO> userUpdate = Wrappers.lambdaUpdate();
        userUpdate.set(SysUserDO::getStatus,status).eq(SysUserDO::getId,userId);
        userMapper.update(userUpdate);

        handleUserUpdateCache(userId);

        LogUtils.logContent(tmp.getName() + " 用户(id:" + tmp.getId() + ")的状态为 " + status);
    }

    @Override
    public void deleteUser(Integer userId) {
        SysUserDO tmp = checkIdExistence(userId);
        checkAdmin(userId);
        userMapper.deleteById(userId);
        ldapPersonRepo.deleteById(EvaLdapUtils.getUserLdapNameId(getUsername(userId)));
        userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getUserId,userId));

        handleUserUpdateCache(userId,tmp.getUsername());

        LogUtils.logContent(tmp.getName() + " 用户(id:" + tmp.getId() + ")");
    }

    @Override
    public void assignRole(Integer userId, List<Integer> roleId) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 退化为委托壳（保持行为不变）
        assignRoleUseCase.execute(userId, roleId);
    }

    @Override
    public void createUser(NewUserCmd cmd) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 退化为委托壳（保持行为不变）
        createUserUseCase.execute(cmd);
    }

    private void checkAdmin(Integer userId) {
        String username = getUsername(userId);
        if ("admin".equalsIgnoreCase(username)) {
            throw new BizException("初始管理员账户不允许此操作");
        }
    }

    private void handleUserUpdateCache(Integer userId) {
        String username = getUsername(userId);
        handleUserUpdateCache(userId,username);
    }

    private void handleUserUpdateCache(Integer userId,String username) {
        cacheManager.invalidateCache(userCacheConstants.ONE_USER_ID , String.valueOf(userId));
        cacheManager.invalidateCache(userCacheConstants.ONE_USER_USERNAME ,username);
        cacheManager.invalidateCache(null,userCacheConstants.ALL_USER);
        cacheManager.invalidateCache(null,userCacheConstants.ALL_USER_USERNAME);
        cacheManager.invalidateCache(userCacheConstants.USER_ROLE , String.valueOf(userId));
        cacheManager.invalidateCache(null,userCacheConstants.ALL_DEPARTMENT);
        cacheManager.invalidateArea(courseCacheConstants.COURSE_LIST_BY_SEM);
    }

    /**
     * 检查id是否存在
     */
    private SysUserDO checkIdExistence(Integer id) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.lambdaQuery();
        query.select(SysUserDO::getId,SysUserDO::getName).eq(SysUserDO::getId,id);
        SysUserDO sysUserDO = userMapper.selectOne(query);
        if (sysUserDO == null) {
            throw new BizException("用户id不存在");
        }
        return sysUserDO;
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
