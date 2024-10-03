package edu.cuit.infra.gateway.impl.user;

import edu.cuit.client.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.gateway.user.RoleQueryGateway;

import java.util.List;
import java.util.Optional;

public class RoleQueryGatewayImpl implements RoleQueryGateway {

    @Override
    public Optional<RoleEntity> getById(Integer roleId) {
        return Optional.empty();
    }

    @Override
    public PaginationResultEntity<RoleEntity> page(PagingQuery<GenericConditionalQuery> query) {
        return null;
    }

    @Override
    public List<SimpleRoleInfoCO> allRole() {
        return List.of();
    }

    @Override
    public List<Integer> getRoleMenuIds(Integer roleId) {
        return List.of();
    }
}
