package edu.cuit.app.service.impl.user;

import com.alibaba.cola.exception.BizException;
import edu.cuit.app.convertor.user.MenuBizConvertor;
import edu.cuit.bc.iam.application.contract.api.user.IMenuService;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.GenericMenuSectionCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.MenuCO;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewMenuCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.bc.iam.application.usecase.CreateMenuUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteMenuUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteMultipleMenuUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateMenuInfoUseCase;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService {

    private final MenuQueryGateway menuQueryGateway;

    private final UpdateMenuInfoUseCase updateMenuInfoUseCase;
    private final CreateMenuUseCase createMenuUseCase;
    private final DeleteMenuUseCase deleteMenuUseCase;
    private final DeleteMultipleMenuUseCase deleteMultipleMenuUseCase;

    private final MenuBizConvertor menuBizConvertor;

    @Override
    @Transactional
    public List<MenuCO> mainMenu(MenuConditionalQuery query) {
        return menuQueryGateway.getMenus(query).stream()
                .map(menuBizConvertor::menuEntityToMenuCO).toList();
    }

    @Override
    @Transactional
    public GenericMenuSectionCO one(Integer id) {
        return menuQueryGateway.getOne(id).map(menuBizConvertor::menuEntityToMenuCO).orElseThrow(() -> new BizException("该菜单不存在"));
    }

    @Override
    @Transactional
    public void update(UpdateMenuCmd updateMenuCmd) {
        updateMenuInfoUseCase.execute(updateMenuCmd);
    }

    @Override
    @Transactional
    public void create(NewMenuCmd newMenuCmd) {
        createMenuUseCase.execute(newMenuCmd);
    }

    @Override
    @Transactional
    public void delete(Integer menuId) {
        deleteMenuUseCase.execute(menuId);
    }

    @Override
    @Transactional
    public void multipleDelete(List<Integer> ids) {
        deleteMultipleMenuUseCase.execute(ids);
    }
}
