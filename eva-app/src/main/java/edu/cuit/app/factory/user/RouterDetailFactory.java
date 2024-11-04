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
                    .stream().filter(menuEntity -> menuEntity.getParentId() == null || menuEntity.getParentId() == 0)
                    .toList();
        } else {
            userMenus = new ArrayList<>();
            for (RoleEntity role : user.getRoles()) {
                userMenus.addAll(role.getMenus().stream()
                        .filter(menuEntity -> menuEntity.getParentId() == null || menuEntity.getParentId() == 0)
                        .toList());
            }
        }

        return userMenus.stream()
                .map((menu) -> toRouterDetailCO(menu,getUserMenus(user).stream()
                        .map(MenuEntity::getId)
                        .toList()))
                .toList();

    }

    private static RouterDetailCO toRouterDetailCO(MenuEntity menuEntity,List<Integer> userMenuIds) {
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
                        .filter(menu -> userMenuIds.contains(menu.getId()))
                        .map((menu) -> toRouterDetailCO(menu,userMenuIds))
                        .toList());
        return routerDetailCO;
    }

    private static List<MenuEntity> getUserMenus(UserEntity user) {
        List<MenuEntity> userMenus = new ArrayList<>();
        for (RoleEntity role : user.getRoles()) {
            userMenus.addAll(role.getMenus().stream()
                    .toList());
        }
        return userMenus;
    }

}
