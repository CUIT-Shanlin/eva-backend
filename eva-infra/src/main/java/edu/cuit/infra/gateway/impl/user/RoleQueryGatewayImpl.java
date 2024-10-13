package edu.cuit.infra.gateway.impl.user;

import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import edu.cuit.client.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.user.RoleConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleMenuDO;
import edu.cuit.infra.dal.database.mapper.user.SysMenuMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.util.QueryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleQueryGatewayImpl implements RoleQueryGateway {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    private final MenuQueryGateway menuQueryGateway;

    private final RoleConverter roleConverter;
    private final PaginationConverter paginationConverter;

    @Override
    public Optional<RoleEntity> getById(Integer roleId) {
        Optional<RoleEntity> roleEntity = Optional.ofNullable(roleConverter.toRoleEntity(roleMapper.selectById(roleId)));
        roleEntity.ifPresent(this::fillRoleEntity);
        return roleEntity;
    }

    @Override
    public PaginationResultEntity<RoleEntity> page(PagingQuery<GenericConditionalQuery> query) {
        Page<SysRoleDO> rolePage = QueryUtils.createPage(query);
        LambdaQueryWrapper<SysRoleDO> roleQuery = Wrappers.lambdaQuery();
        GenericConditionalQuery queryObj = query.getQueryObj();
        QueryUtils.fileTimeQuery(roleQuery,queryObj,SysRoleDO::getCreateTime,SysRoleDO::getUpdateTime);

        roleQuery.or().like(SysRoleDO::getRoleName,queryObj.getKeyword());
        //查询
        Page<SysRoleDO> resultPage = roleMapper.selectPage(rolePage, roleQuery);
        return paginationConverter.toPaginationEntity(rolePage,resultPage.getRecords()
                .stream().map(roleConverter::toRoleEntity).toList());
    }

    @Override
    public List<SimpleRoleInfoCO> allRole() {
        LambdaQueryWrapper<SysRoleDO> roleQuery = Wrappers.lambdaQuery();
        roleQuery.select(SysRoleDO::getId,SysRoleDO::getDescription,SysRoleDO::getRoleName);
        return roleMapper.selectList(roleQuery).stream().map(roleConverter::toSimpleRoleInfo).toList();
    }

    @Override
    public List<Integer> getRoleMenuIds(Integer roleId) {
        MPJLambdaWrapper<SysMenuDO> menuQuery = MPJWrappers.lambdaJoin();
        menuQuery.select(SysMenuDO::getId)
                .innerJoin(SysRoleMenuDO.class,on -> on.eq(SysRoleMenuDO::getRoleId,roleId))
                .eq(SysMenuDO::getId,SysRoleMenuDO::getMenuId);
        return menuMapper.selectList(menuQuery).stream().map(SysMenuDO::getId).toList();
    }

    /**
     * 填充roleEntity字段
     */
    private void fillRoleEntity(RoleEntity roleEntity) {
        roleEntity.setMenus(getRoleMenuIds(roleEntity.getId())
                .stream().map(menuId -> menuQueryGateway.getOne(menuId).orElseThrow(() -> {
                    SysException sysException = new SysException("菜单查询异常，请联系管理员");
                    log.error("菜单查询异常，请联系管理员",sysException);
                    return sysException;
                })).toList());
    }
}
