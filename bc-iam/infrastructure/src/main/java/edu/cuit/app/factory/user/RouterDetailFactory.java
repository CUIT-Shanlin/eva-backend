package edu.cuit.app.factory.user;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.RouterDetailCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.RouterMeta;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.infra.convertor.user.UserConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由规则工厂类
 */
public class RouterDetailFactory {

    public static List<RouterDetailCO> createRouterDetail(Object user) {
        UserConverter userConverter = SpringUtil.getBean(UserConverter.class);
        List<MenuEntity> userMenus;

        if ("admin".equals(userConverter.usernameOf(user, true))) {
            MenuQueryGateway menuQueryGateway = SpringUtil.getBean(MenuQueryGateway.class);
            userMenus = menuQueryGateway.getAllMenu()
                    .stream()
                    .filter(menuEntity -> menuEntity.getParentId() == null || menuEntity.getParentId() == 0)
                    .toList();
        } else {
            userMenus = new ArrayList<>();
            for (RoleEntity role : userConverter.rolesOf(user, true)) {
                if (role.getStatus() == 0) continue;
                userMenus.addAll(role.getMenus().stream()
                        .filter(menuEntity -> menuEntity.getParentId() == null || menuEntity.getParentId() == 0)
                        .filter(menuEntity -> menuEntity.getStatus() == 1)
                        .toList());
            }
        }

        return userMenus.stream()
                .map((menu) -> toRouterDetailCO(menu,getUserMenus(user).stream()
                        .map(MenuEntity::getId)
                        .toList(), userConverter.nameOf(user, true)))
                .toList();

    }

    private static RouterDetailCO toRouterDetailCO(MenuEntity menuEntity,List<Integer> userMenuIds,String username) {
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
                        .filter(menu -> menu.getStatus() == 1 || "admin".equals(username))
                        .map(menu -> toRouterDetailCO(menu,userMenuIds,username))
                        .toList());
        return routerDetailCO;
    }

    private static List<MenuEntity> getUserMenus(Object user) {
        UserConverter userConverter = SpringUtil.getBean(UserConverter.class);
        List<MenuEntity> userMenus = new ArrayList<>();
        for (RoleEntity role : userConverter.rolesOf(user, true)) {
            if (!"admin".equals(userConverter.nameOf(user, true)) && role.getStatus() == 0) continue;
            userMenus.addAll(role.getMenus().stream()
                    .toList());
        }
        return userMenus;
    }

}
