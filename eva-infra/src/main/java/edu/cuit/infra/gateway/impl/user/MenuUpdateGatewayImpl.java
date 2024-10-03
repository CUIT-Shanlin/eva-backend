package edu.cuit.infra.gateway.impl.user;

import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;

import java.util.List;

public class MenuUpdateGatewayImpl implements MenuUpdateGateway {

    @Override
    public void updateMenuInfo(UpdateMenuCmd cmd) {

    }

    @Override
    public void deleteMenu(Integer menuId) {

    }

    @Override
    public void deleteMultipleMenu(List<Integer> menuIds) {

    }

    @Override
    public void createMenu(NewMenuCmd cmd) {

    }
}
