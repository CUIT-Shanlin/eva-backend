package edu.cuit.infra.gateway.impl.user;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.user.UserConverter;
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

    private final UserConverter userConverter;

    @Override
    public Optional<UserEntity> findById(Integer id) {
        SysUserDO userDO = userMapper.selectById(id);
        return Optional.empty();
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return Optional.empty();
    }

    @Override
    public List<UserEntity> page(PagingQuery<GenericConditionalQuery> query) {
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
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageUnqualifiedUserInfo(PagingQuery<UnqualifiedUserConditionalQuery> query) {
        return null;
    }

    @Override
    public Boolean isUsernameExist(String username) {
        return null;
    }
}
