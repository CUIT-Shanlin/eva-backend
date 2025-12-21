package edu.cuit.infra.bciam.adapter;

import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.bc.iam.application.port.UserEntityQueryPort;
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
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.util.QueryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * bc-iam：用户实体查询端口适配器（保持历史行为不变：原样搬运旧 gateway 查询与装配逻辑）。
 *
 * <p>当前收敛目标是把 {@code UserQueryGatewayImpl.fileUserEntity}（角色/菜单装配）从旧 gateway 中抽离，
 * 并让旧 gateway 逐步退化为委托壳；API/异常文案/排序规则保持不变。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEntityQueryPortImpl implements UserEntityQueryPort {

    @Autowired
    @Lazy
    private UserQueryGateway userQueryGateway;

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
        userQuery.eq(SysUserDO::getUsername, username);
        SysUserDO userDO = userMapper.selectOne(userQuery);
        return Optional.ofNullable(fileUserEntity(userDO));
    }

    @Override
    public PaginationResultEntity<UserEntity> page(PagingQuery<GenericConditionalQuery> query) {
        Page<SysUserDO> userPage = Page.of(query.getPage(), query.getSize());
        GenericConditionalQuery queryObj = query.getQueryObj();

        //查询
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        QueryUtils.fileTimeQuery(userQuery, queryObj, SysUserDO::getCreateTime, SysUserDO::getUpdateTime);
        String keyword = queryObj.getKeyword();
        userQuery.and(queryWrp -> {
            queryWrp.like(keyword != null, SysUserDO::getName, keyword)
                    .or().like(keyword != null, SysUserDO::getUsername, keyword);
        });
        userQuery.orderByDesc(SysUserDO::getCreateTime);
        Page<SysUserDO> usersPage = userMapper.selectPage(userPage, userQuery);

        //映射
        List<UserEntity> userEntityList = usersPage.getRecords().stream().map(this::fileUserEntity).toList();

        return paginationConverter.toPaginationEntity(usersPage, userEntityList);
    }

    /**
     * 填充UserEntity字段
     *
     * @param userDO SysUserDO
     */
    private UserEntity fileUserEntity(SysUserDO userDO) {
        //查询角色
        LambdaQueryWrapper<SysRoleDO> roleQuery = new LambdaQueryWrapper<>();
        List<Integer> userRoleIds = userQueryGateway.getUserRoleIds(userDO.getId());
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
                                log.error("菜单查询出错", sysException);
                                return sysException;
                            }))
                            .toList();

                    return roleConverter.toRoleEntity(roleDo, menus);
                }).toList();
        return userConverter.toUserEntity(userDO, userRoles);
    }
}

