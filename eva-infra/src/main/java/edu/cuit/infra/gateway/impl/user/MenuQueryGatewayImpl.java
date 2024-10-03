package edu.cuit.infra.gateway.impl.user;

import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.gateway.user.MenuQueryGateway;

import java.util.List;

public class MenuQueryGatewayImpl implements MenuQueryGateway {

    @Override
    public List<MenuCO> getMenus(MenuConditionalQuery query) {
        return List.of();
    }

    @Override
    public MenuCO getOne(Integer id) {
        return null;
    }
}
