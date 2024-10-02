package edu.cuit.domain.gateway.user;

import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;

/**
 * 权限菜单数据门面
 */
@Component
public interface MenuQueryGateway {

    /**
     * 获取菜单信息
     * @param query 菜单条件查询对象
     */
    List<MenuCO> getMenus(MenuConditionalQuery query);

    /**
     * 获取一个菜单信息
     * @param id 菜单id
     */
    MenuCO getOne(Integer id);

}
