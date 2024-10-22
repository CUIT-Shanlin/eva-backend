package edu.cuit.domain.gateway.user;

import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * 权限菜单数据门面
 */
@Component
public interface MenuQueryGateway {

    /**
     * 获取菜单信息
     * @param query 菜单条件查询对象
     */
    List<MenuEntity> getMenus(MenuConditionalQuery query);

    /**
     * 获取一个菜单信息
     * @param id 菜单id
     */
    Optional<MenuEntity> getOne(Integer id);

    /**
     * 获取菜单的子菜单（包括子菜单的子菜单）
     * @param parentMenuId 父菜单Id
     */
    List<MenuEntity> getChildrenMenus(Integer parentMenuId);

    /**
     * 获取所有菜单
     */
    List<MenuEntity> getAllMenu();

}
