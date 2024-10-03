package edu.cuit.infra.gateway.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.user.MenuConvertor;
import edu.cuit.infra.convertor.user.RoleConverter;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserQueryGatewayImpl implements UserQueryGateway {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    private RoleQueryGateway roleQueryGateway;
    private MenuQueryGateway menuQueryGateway;

    private final UserConverter userConverter;
    private final RoleConverter roleConverter;
    private final MenuConvertor menuConvertor;

    @Override
    public Optional<UserEntity> findById(Integer id) {
        SysUserDO userDO = userMapper.selectById(id);
        return Optional.of(fileUserEntity(userDO));
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        //查询用户
        LambdaQueryWrapper<SysUserDO> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(SysUserDO::getUsername,username);
        SysUserDO userDO = userMapper.selectOne(userQuery);
        return Optional.of(fileUserEntity(userDO));
    }

    @Override
    public List<UserEntity> page(PagingQuery<GenericConditionalQuery> query) {

        Page<SysUserDO> userPage = new Page<>();
        return List.of();
    }

    @Override
    public List<SimpleResultCO> allUser() {
        return List.of();
    }

    @Override
    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUser() {
        return null;
    }

    @Override
    public List<Integer> getUserRoles(Integer userId) {
        return List.of();
    }

    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageUnqualifiedUserInfo(PagingQuery<UnqualifiedUserConditionalQuery> query) {
        return null;
    }

    @Override
    public Boolean isUsernameExist(String username) {
        return null;
    }

    /**
     * 填充UserEntity字段
     * @param userDO SysUserDO
     */
    private UserEntity fileUserEntity(SysUserDO userDO) {
        //查询角色
        LambdaQueryWrapper<SysRoleDO> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.in(SysRoleDO::getId,getUserRoles(userDO.getId()));
        List<RoleEntity> userRoles = roleMapper.selectList(roleQuery).stream()
                .map(roleDo -> {

                    //查询角色权限菜单
                    List<MenuEntity> menus = roleQueryGateway.getRoleMenuIds(roleDo.getId())
                            .stream()
                            .map(menuQueryGateway::getOne).toList();

                    return roleConverter.toRoleEntity(roleDo,menus);
                }).toList();
        return userConverter.toUserEntity(userDO,userRoles);
    }
}
