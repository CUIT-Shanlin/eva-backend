package edu.cuit.app.service.impl.user;

import com.alibaba.cola.exception.BizException;
import edu.cuit.app.convertor.user.MenuBizConvertor;
import edu.cuit.client.api.user.IMenuService;
import edu.cuit.client.dto.clientobject.user.GenericMenuSectionCO;
import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService {

    private final MenuQueryGateway menuQueryGateway;
    private final MenuUpdateGateway menuUpdateGateway;

    private final MenuBizConvertor menuBizConvertor;

    @Override
    public List<MenuCO> mainMenu(MenuConditionalQuery query) {
        return menuQueryGateway.getMenus(query).stream()
                .map(menuBizConvertor::menuEntityToMenuCO).toList();
    }

    @Override
    public GenericMenuSectionCO one(Integer id) {
        return menuQueryGateway.getOne(id).map(menuBizConvertor::menuEntityToMenuCO).orElseThrow(() -> new BizException("该菜单不存在"));
    }

    @Override
    public void update(UpdateMenuCmd updateMenuCmd) {
        menuUpdateGateway.updateMenuInfo(updateMenuCmd);
    }

    @Override
    public void create(NewMenuCmd newMenuCmd) {
        menuUpdateGateway.createMenu(newMenuCmd);
    }

    @Override
    public void delete(Integer menuId) {
        menuUpdateGateway.deleteMenu(menuId);
    }

    @Override
    public void multipleDelete(List<Integer> ids) {
        menuUpdateGateway.deleteMultipleMenu(ids);
    }
}
