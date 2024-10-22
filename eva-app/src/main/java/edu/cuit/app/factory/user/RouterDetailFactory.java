package edu.cuit.app.factory.user;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.client.dto.clientobject.user.RouterDetailCO;
import edu.cuit.client.dto.clientobject.user.RouterMeta;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由规则工厂类
 */
public class RouterDetailFactory {

    public static List<RouterDetailCO> createRouterDetail(UserEntity user) {
        List<MenuEntity> userMenus;

        if ("admin".equals(user.getUsername())) {
            MenuQueryGateway menuQueryGateway = SpringUtil.getBean(MenuQueryGateway.class);
            userMenus = menuQueryGateway.getAllMenu()
                    .stream().filter(menuEntity -> menuEntity.getParentId() == null)
                    .toList();
        } else {
            userMenus = new ArrayList<>();
            for (RoleEntity role : user.getRoles()) {
                userMenus.addAll(role.getMenus().stream()
                        .filter(menuEntity -> menuEntity.getParentId() == null)
                        .toList());
            }
        }

        return userMenus.stream()
                .map(RouterDetailFactory::toRouterDetailCO)
                .toList();

    }



    private static RouterDetailCO toRouterDetailCO(MenuEntity menuEntity) {
        RouterDetailCO routerDetailCO = new RouterDetailCO();
        routerDetailCO
                .setPath(menuEntity.getPath())
                .setComponent(menuEntity.getComponent())
                .setAlwaysShow(menuEntity.getType() == 0)
                .setHidden(menuEntity.getType() == 2)
                .setMeta(new RouterMeta()
                        .setIcon(menuEntity.getIcon())
                        .setName(menuEntity.getName()))
                .setChildren(menuEntity.getChildren().stream()
                        .map(RouterDetailFactory::toRouterDetailCO)
                        .toList());
        return routerDetailCO;
    }

}
