package edu.cuit.infra.gateway.impl.user;

import edu.cuit.bc.iam.application.usecase.CreateMenuUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteMenuUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteMultipleMenuUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateMenuInfoUseCase;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCacheInvalidate;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCacheInvalidateContainer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class MenuUpdateGatewayImpl implements MenuUpdateGateway {

    private final UpdateMenuInfoUseCase updateMenuInfoUseCase;
    private final DeleteMenuUseCase deleteMenuUseCase;
    private final DeleteMultipleMenuUseCase deleteMultipleMenuUseCase;
    private final CreateMenuUseCase createMenuUseCase;

    @Override
    @LocalCacheInvalidateContainer({
            @LocalCacheInvalidate(area = "#{@userCacheConstants.ONE_MENU}",key = "#cmd.id"),
            @LocalCacheInvalidate(area = "#{@userCacheConstants.MENU_CHILDREN}",key = "#cmd.parentId")
    })
    public void updateMenuInfo(UpdateMenuCmd cmd) {
        updateMenuInfoUseCase.execute(cmd);
    }

    @Override
    public void deleteMenu(Integer menuId) {
        deleteMenuUseCase.execute(menuId);
    }

    @Override
    public void deleteMultipleMenu(List<Integer> menuIds) {
        deleteMultipleMenuUseCase.execute(menuIds);
    }

    @Override
    @LocalCacheInvalidateContainer({
            @LocalCacheInvalidate(area = "#{@userCacheConstants.MENU_CHILDREN}", key = "#cmd.parentId"),
            @LocalCacheInvalidate(area = "#{@userCacheConstants.ONE_MENU}", key = "#cmd.parentId")
    })
    public void createMenu(NewMenuCmd cmd) {
        createMenuUseCase.execute(cmd);
    }

}
