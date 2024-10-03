package edu.cuit.infra.gateway.impl.user;

import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;

import java.util.List;

public class MenuQueryGatewayImpl implements MenuQueryGateway {

    @Override
    public List<MenuEntity> getMenus(MenuConditionalQuery query) {
        return List.of();
    }

    @Override
    public MenuEntity getOne(Integer id) {
        return null;
    }
}
