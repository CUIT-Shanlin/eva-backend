package edu.cuit.infra.enums.cache;

import org.springframework.stereotype.Component;

/**
 * 缓存键常量
 */
@Component("userCacheConstants")
public class UserCacheConstants {

//    权限相关

    // 单个菜单(ID)
    public final String ONE_MENU = "menu.one";

    // 某id菜单的子菜单
    public final String MENU_CHILDREN = "menu.children";

    // 所有菜单信息
    public final String ALL_MENU = "menu.all";

    // 角色权限
    public final String ROLE_MENU = "role.perm";

//    角色相关

    // 单个角色(ID)
    public final String ONE_ROLE = "role.one";

    // 所有角色
    public final String ALL_ROLE = "role.all";

    // 默认角色
    public final String DEFAULT_ROLE = "role.default";

    // 用户角色
    public final String USER_ROLE = "user.role";

//    用户相关

    // 单个用户(ID)
    public final String ONE_USER_ID = "user.one.id";

    // 单个用户(ID)
    public final String ONE_USER_USERNAME = "user.one.username";

    // 所有用户id
    public final String ALL_USER_ID = "user.all.id";

    // 所有用户用户名
    public final String ALL_USER_USERNAME = "user.all.username";

    // 所有用户
    public final String ALL_USER = "user.all";

    // 所有专业
    public final String ALL_DEPARTMENT = "department.all";

}
