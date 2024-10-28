package edu.cuit.infra.gateway.impl.user;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.user.RoleConverter;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.*;
import edu.cuit.infra.util.QueryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserQueryGatewayImpl implements UserQueryGateway {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    private final RoleQueryGateway roleQueryGateway;
    private final MenuQueryGateway menuQueryGateway;

    private final UserConverter userConverter;
    private final RoleConverter roleConverter;
    private final PaginationConverter paginationConverter;

    @Override
    public Optional<UserEntity> findById(Integer id) {
        SysUserDO userDO = userMapper.selectById(id);
        return Optional.ofNullable(fileUserEntity(userDO));
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        //查询用户
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.eq(SysUserDO::getUsername,username);
        SysUserDO userDO = userMapper.selectOne(userQuery);
        return Optional.ofNullable(fileUserEntity(userDO));
    }

    @Override
    public Optional<Integer> findIdByUsername(String username) {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId)
                .eq(SysUserDO::getUsername,username);
        return Optional.ofNullable(userMapper.selectOne(userQuery)).map(SysUserDO::getId);
    }

    @Override
    public Optional<String> findUsernameById(Integer id) {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getUsername)
                .eq(SysUserDO::getId,id);
        return Optional.ofNullable(userMapper.selectOne(userQuery)).map(SysUserDO::getUsername);
    }

    @Override
    public List<Integer> findAllUserId() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId);
        return userMapper.selectList(userQuery).stream().map(SysUserDO::getId).toList();
    }

    @Override
    public List<String> findAllUsername() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getUsername);
        return userMapper.selectList(userQuery).stream().map(SysUserDO::getUsername).toList();
    }

    @Override
    public PaginationResultEntity<UserEntity> page(PagingQuery<GenericConditionalQuery> query) {
        Page<SysUserDO> userPage = Page.of(query.getPage(),query.getSize());
        GenericConditionalQuery queryObj = query.getQueryObj();

        //查询
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        QueryUtils.fileTimeQuery(userQuery,queryObj,SysUserDO::getCreateTime,SysUserDO::getUpdateTime);
        String keyword = queryObj.getKeyword();
        userQuery
                .like(keyword != null,SysUserDO::getName,keyword)
                .or().like(keyword != null,SysUserDO::getUsername,keyword);
        Page<SysUserDO> usersPage = userMapper.selectPage(userPage, userQuery);

        //映射
        List<UserEntity> userEntityList = usersPage.getRecords().stream().map(this::fileUserEntity).toList();

        return paginationConverter.toPaginationEntity(usersPage,userEntityList);
    }

    @Override
    public List<SimpleResultCO> allUser() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId,SysUserDO::getName);
        return userMapper.selectList(userQuery).stream().map(userConverter::toUserSimpleResult).toList();
    }

    @Override
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
        return userMapper.selectCount(Wrappers.lambdaQuery(SysUserDO.class)
                .select(SysUserDO::getUsername).eq(SysUserDO::getUsername,username)) >= 1;
    }

    /**
     * 填充UserEntity字段
     * @param userDO SysUserDO
     */
    private UserEntity fileUserEntity(SysUserDO userDO) {
        //查询角色
        LambdaQueryWrapper<SysRoleDO> roleQuery = new LambdaQueryWrapper<>();
        List<Integer> userRoleIds = getUserRoleIds(userDO.getId());
        List<SysRoleDO> roles;
        if (userRoleIds.isEmpty()) {
            roles = List.of();
        } else {
            roleQuery.in(SysRoleDO::getId, userRoleIds);
            roles = roleMapper.selectList(roleQuery);
        }
        Supplier<List<RoleEntity>> userRoles = () -> roles.stream()
                .map(roleDo -> {

                    //查询角色权限菜单
                    Supplier<List<MenuEntity>> menus = () -> roleQueryGateway.getRoleMenuIds(roleDo.getId())
                            .stream()
                            .map(menuId -> menuQueryGateway.getOne(menuId).orElseThrow(() -> {
                                SysException sysException = new SysException("菜单查询出错，请联系管理员");
                                log.error("菜单查询出错",sysException);
                                return sysException;
                            }))
                            .toList();

                    return roleConverter.toRoleEntity(roleDo,menus);
                }).toList();
        return userConverter.toUserEntity(userDO,userRoles);
    }
}
