package edu.cuit.infra.gateway.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import edu.cuit.bc.iam.application.usecase.FindUserByIdUseCase;
import edu.cuit.bc.iam.application.usecase.FindUserByUsernameUseCase;
import edu.cuit.bc.iam.application.usecase.PageUserUseCase;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.*;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCached;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserQueryGatewayImpl implements UserQueryGateway {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    private final UserConverter userConverter;

    private final LocalCacheManager cacheManager;
    private final UserCacheConstants userCacheConstants;

    private final FindUserByIdUseCase findUserByIdUseCase;
    private final FindUserByUsernameUseCase findUserByUsernameUseCase;
    private final PageUserUseCase pageUserUseCase;

    @Override
    @LocalCached(area = "#{@userCacheConstants.ONE_USER_ID}",key = "#id")
    public Optional<UserEntity> findById(Integer id) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return findUserByIdUseCase.execute(id);
    }

    @Override
    @LocalCached(area = "#{@userCacheConstants.ONE_USER_USERNAME}",key = "#username")
    public Optional<UserEntity> findByUsername(String username) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return findUserByUsernameUseCase.execute(username);
    }

    @Override
    public Optional<Integer> findIdByUsername(String username) {

        Optional<UserEntity> cachedUser = cacheManager.getCache(userCacheConstants.ONE_USER_USERNAME ,username);

        if (cachedUser != null && cachedUser.isPresent()) {
            return Optional.ofNullable(cachedUser.get().getId());
        }

        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId)
                .eq(SysUserDO::getUsername,username);
        return Optional.ofNullable(userMapper.selectOne(userQuery)).map(SysUserDO::getId);
    }

    @Override
    public Optional<String> findUsernameById(Integer id) {

        Optional<UserEntity> cachedUser = cacheManager.getCache(userCacheConstants.ONE_USER_ID , String.valueOf(id));

        if (cachedUser != null && cachedUser.isPresent()) {
            return Optional.ofNullable(cachedUser.get().getUsername());
        }

        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getUsername)
                .eq(SysUserDO::getId,id);
        return Optional.ofNullable(userMapper.selectOne(userQuery)).map(SysUserDO::getUsername);
    }

    @Override
    @LocalCached(key = "#{@userCacheConstants.ALL_USER_ID}")
    public List<Integer> findAllUserId() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId);
        return userMapper.selectList(userQuery).stream().map(SysUserDO::getId).toList();
    }

    @Override
    @LocalCached(key = "#{@userCacheConstants.ALL_USER_USERNAME}")
    public List<String> findAllUsername() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getUsername);
        return userMapper.selectList(userQuery).stream().map(SysUserDO::getUsername).toList();
    }

    @Override
    public PaginationResultEntity<UserEntity> page(PagingQuery<GenericConditionalQuery> query) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return pageUserUseCase.execute(query);
    }

    @Override
    @LocalCached(key = "#{@userCacheConstants.ALL_USER}")
    public List<SimpleResultCO> allUser() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId,SysUserDO::getName);
        return userMapper.selectList(userQuery).stream().map(userConverter::toUserSimpleResult).toList();
    }

    @Override
    @LocalCached(area = "#{@userCacheConstants.USER_ROLE}",key = "#userId")
    public List<Integer> getUserRoleIds(Integer userId) {
        MPJLambdaWrapper<SysRoleDO> roleQuery = MPJWrappers.lambdaJoin();
        roleQuery
                .select(SysRoleDO::getId)
                .innerJoin(SysUserRoleDO.class,on -> on
                        .eq(SysUserRoleDO::getUserId,userId))
                .eq(SysRoleDO::getId,SysUserRoleDO::getRoleId);

        return roleMapper.selectList(roleQuery).stream().map(SysRoleDO::getId).toList();
    }

    @Override
    public Boolean isUsernameExist(String username) {

        Optional<UserEntity> cachedUser = cacheManager.getCache(userCacheConstants.ONE_USER_USERNAME ,username);
        if (cachedUser != null && cachedUser.isPresent()) {
            return true;
        }

        return userMapper.exists(Wrappers.lambdaQuery(SysUserDO.class)
                .select(SysUserDO::getUsername).eq(SysUserDO::getUsername,username));
    }

    @Override
    public Optional<Integer> getUserStatus(Integer id) {

        Optional<UserEntity> cachedUser = cacheManager.getCache(userCacheConstants.ONE_USER_ID , String.valueOf(id));
        if (cachedUser != null && cachedUser.isPresent()) {
            return Optional.ofNullable(cachedUser.get().getStatus());
        }

        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getStatus)
                .eq(SysUserDO::getId,id);
        return Optional.ofNullable(userMapper.selectOne(userQuery).getStatus());
    }

}
