package edu.cuit.domain.gateway.user;

import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 菜单更新数据门户
 */
@Component
public interface MenuUpdateGateway {

    /**
     * 修改菜单信息
     * @param cmd 修改请求对象
     */
    void updateMenuInfo(UpdateMenuCmd cmd);

    /**
     * 删除单个菜单
     * @param menuId 菜单id
     */
    void deleteMenu(Integer menuId);

    /**
     * 批量删除菜单
     * @param menuIds 菜单idList
     */
    void deleteMultipleMenu(List<Integer> menuIds);

    /**
     * 新建菜单
     * @param cmd 新建菜单请求对象
     */
    void createMenu(NewMenuCmd cmd);

}
