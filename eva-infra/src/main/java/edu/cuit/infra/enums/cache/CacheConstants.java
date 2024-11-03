package edu.cuit.infra.enums.cache;

import org.springframework.stereotype.Component;

/**
 * 缓存键常量
 */
@Component("cacheConstants")
public class CacheConstants {

    // 单个菜单
    public final String ONE_MENU = "menu.one.";

    // 某id菜单的子菜单
    public final String MENU_CHILDREN = "menu.children.";

    // 角色权限
    public final String ROLE_MENU = "role.perm.";
}
